// E:\WorkshopPetShelter\app\src\main\java\com\example\workshoppetshelter\UserDbHelper.java
package com.example.workshoppetshelter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Phase 3: SQLite layer only. No UI, no Activity, no navigation.
 *
 * Passwords are NEVER stored in plaintext. We store:
 *   - a random 16-byte salt (Base64, NO_WRAP)
 *   - a 256-bit PBKDF2WithHmacSHA256 hash (Base64, NO_WRAP)
 * OWASP recommends a slow KDF with a unique salt per account. 120000 iterations
 * is a workshop-safe compromise: slow enough against offline guessing, fast
 * enough on a phone/emulator. A production app should re-evaluate this value
 * every couple of years and store it with the hash for future migrations.
 */
public class UserDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "workshop_pet_shelter.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_PETS = "pets";

    // Crypto parameters -- deliberately explicit so they can be reviewed by a professor.
    private static final int SALT_LENGTH_BYTES = 16;   // 128-bit salt
    private static final int KEY_LENGTH_BITS = 256;    // 32-byte derived key
    private static final int PBKDF2_ITERATIONS = 120000;

    // SecureRandom is a CSPRNG. Never use java.util.Random for salts: it is predictable.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String CREATE_USERS =
            "CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "full_name TEXT NOT NULL, " +
                    "email TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                    "password_hash TEXT NOT NULL, " +
                    "salt TEXT NOT NULL, " +
                    "role TEXT NOT NULL CHECK (role IN ('ADMIN', 'USER'))" +
                    ")";

    // NOTE: SQLite does NOT enforce FOREIGN KEY constraints unless PRAGMA foreign_keys = ON
    // is executed on EVERY open connection. We intentionally do NOT enable it in this
    // workshop phase, to avoid breaking beginner flows when a user is deleted.
    private static final String CREATE_PETS =
            "CREATE TABLE pets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "species TEXT NOT NULL, " +
                    "age INTEGER NOT NULL CHECK (age >= 0), " +
                    "status TEXT NOT NULL DEFAULT 'AVAILABLE' " +
                    "CHECK (status IN ('AVAILABLE', 'PENDING', 'ADOPTED')), " +
                    "requested_by INTEGER, " +
                    "FOREIGN KEY (requested_by) REFERENCES users(id)" +
                    ")";

    private static final String[] USER_COLUMNS_WITH_SECRET = {
            "id", "full_name", "email", "password_hash", "salt", "role"
    };

    private static final String[] USER_COLUMNS_PUBLIC = {
            "id", "full_name", "email", "role"
    };

    private static final String PET_SELECT =
            "p.id AS id, " +
                    "p.name AS name, " +
                    "p.species AS species, " +
                    "p.age AS age, " +
                    "p.status AS status, " +
                    "u.full_name AS requested_name";

    public UserDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS);
        // Admin must exist after the users table and before any pet that could reference a user.
        // Phase 8.5: presentation account for the professor.
        insertAccount(db, "Wajih Khadraoui", "khadraouiwajih3@gmail.com", "wajih@123", Role.ADMIN);

        db.execSQL(CREATE_PETS);
        insertDefaultPet(db, "Rex", "Chien", 3);
        insertDefaultPet(db, "Minou", "Chat", 2);
        insertDefaultPet(db, "Bunny", "Lapin", 1);
        // Phase 8.5: three extra seed pets for the demo (6 total).
        insertDefaultPet(db, "Luna", "Chat", 4);
        insertDefaultPet(db, "Max", "Chien", 5);
        insertDefaultPet(db, "Coco", "Perroquet", 2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // WORKSHOP ONLY: dropping tables loses all data. Acceptable here because this is a
        // university exercise with seed data. A real app would run ALTER TABLE migrations.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PETS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /** Creates a fresh 128-bit salt and encodes it with Base64 NO_WRAP for SQLite TEXT storage. */
    private String createSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    /**
     * Derives a 256-bit key from password + salt with PBKDF2WithHmacSHA256.
     * SHA-256(password) alone is fast and vulnerable to rainbow tables / GPU cracking;
     * PBKDF2 deliberately burns 120000 HMAC rounds so each guess is expensive.
     */
    private String hashPassword(String password, String saltText) throws Exception {
        byte[] salt = Base64.decode(saltText, Base64.NO_WRAP);
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    /**
     * Constant-time comparison. String.equals() exits at the first differing character,
     * leaking the position of the match through timing. Here:
     *   - '^' (XOR) yields 0 only when two bytes are identical, non-zero otherwise.
     *   - '|' accumulates every difference into ONE variable without branching.
     *   - length difference is folded in with XOR, so we never early-exit on length either.
     * The loop always runs over the longer string, so execution time does not depend on
     * how many characters matched.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);

        int diff = left.length ^ right.length;
        int max = Math.max(left.length, right.length);
        for (int i = 0; i < max; i++) {
            byte leftByte = i < left.length ? left[i] : 0;
            byte rightByte = i < right.length ? right[i] : 0;
            diff |= leftByte ^ rightByte;
        }
        return diff == 0;
    }

    /** Inserts a seeded account. insertOrThrow makes a duplicate admin email fail loudly. */
    private void insertAccount(SQLiteDatabase db, String name, String email,
                               String password, Role role) {
        String salt = createSalt();
        String hash;
        try {
            hash = hashPassword(password, salt);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password for seeded account", e);
        }

        ContentValues values = new ContentValues();
        values.put("full_name", name == null ? "" : name.trim());
        values.put("email", normalizeEmail(email));
        values.put("password_hash", hash);
        values.put("salt", salt);
        values.put("role", role.name());
        db.insertOrThrow(TABLE_USERS, null, values);
    }

    private void insertDefaultPet(SQLiteDatabase db, String name, String species, int age) {
        ContentValues values = new ContentValues();
        values.put("name", name == null ? "" : name.trim());
        values.put("species", species == null ? "" : species.trim());
        values.put("age", age);
        // status/requested_by are omitted on purpose: SQLite DEFAULT 'AVAILABLE' applies.
        db.insertOrThrow(TABLE_PETS, null, values);
    }

    /** Locale.ROOT avoids the Turkish-I bug when lowercasing an email. */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** @return true if inserted, false if the email already exists (no crash). */
    public boolean register(String name, String email, String password) {
        String salt = createSalt();
        String hash;
        try {
            hash = hashPassword(password, salt);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password for registration", e);
        }

        ContentValues values = new ContentValues();
        values.put("full_name", name == null ? "" : name.trim());
        values.put("email", normalizeEmail(email));
        values.put("password_hash", hash);
        values.put("salt", salt);
        values.put("role", Role.USER.name());

        long rowId = getWritableDatabase().insert(TABLE_USERS, null, values);
        return rowId != -1;
    }

    /**
     * @return the logged-in profile, or null for ANY failure: unknown email, wrong password,
     * corrupted hash or crypto error. Both failures look identical to the caller, so an
     * attacker cannot enumerate registered emails by measuring the response.
     */
    public UserProfile login(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                USER_COLUMNS_WITH_SECRET,
                "email = ?",
                new String[]{normalizeEmail(email)},
                null,
                null,
                null,
                "1")) {

            if (!cursor.moveToFirst()) {
                return null;
            }

            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
            String storedSalt = cursor.getString(cursor.getColumnIndexOrThrow("salt"));
            String candidateHash = hashPassword(password, storedSalt);

            if (!constantTimeEquals(storedHash, candidateHash)) {
                return null;
            }

            return new UserProfile(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    Role.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("role")))
            );
        } catch (Exception e) {
            return null;
        }
    }

    /** Public user list: password_hash and salt are deliberately NOT selected. */
    public List<UserProfile> getAllUsers() {
        List<UserProfile> users = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS, USER_COLUMNS_PUBLIC, null, null, null, null, "id ASC")) {
            while (cursor.moveToNext()) {
                users.add(new UserProfile(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        Role.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("role")))
                ));
            }
        }
        return users;
    }

    public long addPet(String name, String species, int age) {
        ContentValues values = new ContentValues();
        values.put("name", name == null ? "" : name.trim());
        values.put("species", species == null ? "" : species.trim());
        values.put("age", age);
        // No status/requested_by: SQL DEFAULT 'AVAILABLE' + NULL handle both.
        return getWritableDatabase().insertOrThrow(TABLE_PETS, null, values);
    }

    public List<Pet> getAllPets() {
        return queryPets(null, null);
    }

    public List<Pet> getAvailablePets() {
        return queryPets("p.status = ?", new String[]{"AVAILABLE"});
    }

    public List<Pet> getPetsRequestedBy(long userId) {
        return queryPets("p.requested_by = ?", new String[]{String.valueOf(userId)});
    }

    /**
     * Level 3: SQL aggregation. The database counts matching rows and returns ONE value,
     * so we do not load every pet into Java memory just to call size(). A missing user_id
     * simply matches no rows and returns 0.
     */
    public int countAdoptedPetsByUser(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM pets WHERE requested_by = ? AND status = 'ADOPTED'",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    /**
     * Level 3: admin adoption history. Reuses the shared JOIN query instead of copying
     * its aliases, status handling and ordering (DRY).
     */
    public List<Pet> getAdoptedPets() {
        return queryPets("status = ?", new String[]{"ADOPTED"});
    }

    /**
     * Shared JOIN query. 'selection' is ALWAYS one of our own constant strings containing
     * placeholders; user values travel in 'args' and are never concatenated into SQL.
     */
    private List<Pet> queryPets(String selection, String[] args) {
        String sql = "SELECT " + PET_SELECT + " FROM " + TABLE_PETS + " p "
                + "LEFT JOIN " + TABLE_USERS + " u ON p.requested_by = u.id";
        if (selection != null && !selection.isEmpty()) {
            sql += " WHERE " + selection;
        }
        sql += " ORDER BY p.id ASC";

        List<Pet> pets = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                int requestedNameIndex = cursor.getColumnIndexOrThrow("requested_name");
                String requestedBy = cursor.isNull(requestedNameIndex)
                        ? null
                        : cursor.getString(requestedNameIndex);

                pets.add(new Pet(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("species")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("age")),
                        cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        requestedBy
                ));
            }
        }
        return pets;
    }

    /** The status guard makes the update atomic: two clicks cannot both win. */
    public boolean requestAdoption(long petId, long userId) {
        ContentValues values = new ContentValues();
        values.put("status", "PENDING");
        values.put("requested_by", userId);
        int rows = getWritableDatabase().update(
                TABLE_PETS,
                values,
                "id = ? AND status = 'AVAILABLE'",
                new String[]{String.valueOf(petId)}
        );
        return rows > 0;
    }

    public boolean approveAdoption(long petId) {
        ContentValues values = new ContentValues();
        values.put("status", "ADOPTED");
        int rows = getWritableDatabase().update(
                TABLE_PETS,
                values,
                "id = ? AND status = 'PENDING'",
                new String[]{String.valueOf(petId)}
        );
        return rows > 0;
    }

    public boolean rejectAdoption(long petId) {
        ContentValues values = new ContentValues();
        values.put("status", "AVAILABLE");
        values.putNull("requested_by");
        int rows = getWritableDatabase().update(
                TABLE_PETS,
                values,
                "id = ? AND status = 'PENDING'",
                new String[]{String.valueOf(petId)}
        );
        return rows > 0;
    }

    public boolean deletePet(long petId) {
        return getWritableDatabase().delete(
                TABLE_PETS, "id = ?", new String[]{String.valueOf(petId)}) > 0;
    }

    /**
     * Phase 7 addition: fetch a single pet with the SAME LEFT JOIN used by queryPets(),
     * so Pet.requestedBy contains the adopter's full name (or null when nobody requested it).
     * Reusing queryPets avoids duplicating the join/alias/null-handling logic.
     *
     * @return the pet, or null when no row has this id.
     */
    public Pet getPetById(long petId) {
        List<Pet> pets = queryPets("p.id = ?", new String[]{String.valueOf(petId)});
        return pets.isEmpty() ? null : pets.get(0);
    }
}
