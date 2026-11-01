
package com.example.autofilemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoManage {

    private final Activity activity;
    private final Handler mainHandler;
    private final ExecutorService executor;

    // Files recommended for cleanup
    private final ArrayList<Candidate> candidates = new ArrayList<>();
    private final Set<String> candidatePaths = new HashSet<>();

    // Files skipped during scanning but available for a deletion attempt
    private final ArrayList<File> skippedFiles = new ArrayList<>();
    private final ArrayList<String> skippedReasons = new ArrayList<>();
    private final Set<String> skippedPaths = new HashSet<>();

    // Files that failed deletion
    private final ArrayList<File> failedFiles = new ArrayList<>();
    private final Set<String> failedPaths = new HashSet<>();

    // Protected folders and other informational skipped items
    private final ArrayList<String> skippedItems = new ArrayList<>();

    private int skippedCount = 0;

    // Temporary files older than 7 days
    private static final long TEMP_FILE_AGE =
            7L * 24 * 60 * 60 * 1000;

    // Avoid hashing extremely large files
    private static final long MAX_DUPLICATE_CHECK_SIZE =
            100L * 1024 * 1024;

    public AutoManage(Activity activity) {

        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    // =========================================================
    // 1. START SCANNING
    // =========================================================

    public void startAutoScan() {

        if (activity == null || activity.isFinishing()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && !Environment.isExternalStorageManager()) {

            showStoragePermissionDialog();
            return;
        }

        candidates.clear();
        candidatePaths.clear();

        skippedFiles.clear();
        skippedReasons.clear();
        skippedPaths.clear();

        failedFiles.clear();
        failedPaths.clear();

        skippedItems.clear();
        skippedCount = 0;

        Toast.makeText(
                activity,
                "Scanning files. Please wait...",
                Toast.LENGTH_SHORT
        ).show();

        executor.execute(() -> {

            File root = Environment.getExternalStorageDirectory();

            if (root != null && root.exists()) {

                scanDirectory(root);
                findDuplicateFiles(root);
            }

            mainHandler.post(() -> {

                if (activity.isFinishing()) {
                    return;
                }

                showScanResult();
            });
        });
    }

    // =========================================================
    // 2. STORAGE PERMISSION
    // =========================================================

    private void showStoragePermissionDialog() {

        new AlertDialog.Builder(activity)
                .setTitle("Storage Permission Required")
                .setMessage(
                        "Auto File Manager needs All Files Access " +
                                "permission to scan shared storage.\n\n" +
                                "Please enable the permission in Android settings."
                )
                .setPositiveButton("Grant Permission", (dialog, which) -> {

                    try {

                        Intent intent = new Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                        );

                        intent.setData(
                                Uri.parse("package:" + activity.getPackageName())
                        );

                        activity.startActivity(intent);

                    } catch (Exception e) {

                        try {

                            Intent intent = new Intent(
                                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            );

                            activity.startActivity(intent);

                        } catch (Exception ex) {

                            Toast.makeText(
                                    activity,
                                    "Unable to open storage settings",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================
    // 3. SCAN RESULT
    // =========================================================

    private void showScanResult() {

        if (candidates.isEmpty()) {

            if (!skippedFiles.isEmpty() || !skippedItems.isEmpty()) {

                new AlertDialog.Builder(activity)
                        .setTitle("Scan Completed")
                        .setMessage(
                                "No recommended cleanup files were found.\n\n"
                                        + "Skipped files: " + skippedFiles.size() + "\n"
                                        + "Skipped folders/items: " + skippedItems.size()
                        )
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Review Skipped", (dialog, which) -> {
                            showSkippedFilesDialog();
                        })
                        .show();

            } else {

                new AlertDialog.Builder(activity)
                        .setTitle("Scan Completed")
                        .setMessage("No unnecessary files were found.")
                        .setPositiveButton("OK", null)
                        .show();
            }

            return;
        }

        showReviewDialog();
    }

    // =========================================================
    // 4. PROTECTED DIRECTORIES
    // =========================================================

    private boolean isProtectedDirectory(File directory) {

        if (directory == null) {
            return true;
        }

        String path = directory.getAbsolutePath()
                .replace("\\", "/")
                .toLowerCase(Locale.ROOT);

        String name = directory.getName();

        // Android protected folders
        if (path.contains("/android/data")
                || path.contains("/android/obb")) {

            return true;
        }

        // Hidden folders are not scanned,
        // except .thumbnails because it may contain cache files.
        if (name.startsWith(".")
                && !name.equalsIgnoreCase(".thumbnails")) {

            return true;
        }

        return false;
    }

    // =========================================================
    // 5. DIRECTORY SCANNING
    // =========================================================

    private void scanDirectory(File directory) {

        if (directory == null || !directory.exists()) {
            return;
        }

        File[] files;

        try {

            files = directory.listFiles();

        } catch (SecurityException e) {

            addSkippedInfo(
                    directory.getAbsolutePath()
                            + " - Permission denied"
            );

            return;
        }

        if (files == null) {

            addSkippedInfo(
                    directory.getAbsolutePath()
                            + " - Cannot access directory"
            );

            return;
        }

        for (File file : files) {

            if (file == null) {
                continue;
            }

            try {

                if (file.isDirectory()) {

                    if (isProtectedDirectory(file)) {

                        addSkippedInfo(
                                file.getAbsolutePath()
                                        + " - Protected or hidden folder"
                        );

                        continue;
                    }

                    scanDirectory(file);

                } else if (file.isFile()) {

                    checkFileForCleanup(file);
                }

            } catch (SecurityException e) {

                addSkippedFile(
                        file,
                        "Permission denied during scanning"
                );

            } catch (Exception e) {

                addSkippedFile(
                        file,
                        "Unable to scan this file"
                );
            }
        }
    }

    // =========================================================
    // 6. CHECK FILES FOR CLEANUP
    // =========================================================

    private void checkFileForCleanup(File file) {

        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }

        String name = file.getName().toLowerCase(Locale.ROOT);

        String path = file.getAbsolutePath()
                .replace("\\", "/")
                .toLowerCase(Locale.ROOT);

        // Never delete .nomedia
        if (name.equals(".nomedia")) {
            return;
        }

        // Thumbnail cache
        if (path.contains("/.thumbnails/")
                || name.endsWith(".thumb")
                || name.startsWith("thumb_")) {

            addCandidate(file, "Thumbnail cache");
            return;
        }

        // Old temporary and backup files
        boolean temporaryFile =
                name.endsWith(".tmp")
                        || name.endsWith(".temp")
                        || name.endsWith(".bak")
                        || name.endsWith(".part")
                        || name.endsWith(".log");

        if (temporaryFile) {

            long age = System.currentTimeMillis()
                    - file.lastModified();

            if (age >= TEMP_FILE_AGE) {

                addCandidate(file, "Old temporary/backup file");
            }

            return;
        }

        // Empty files
        if (file.length() == 0) {

            addCandidate(file, "Empty file");
        }
    }

    // =========================================================
    // 7. ADD CLEANUP CANDIDATE
    // =========================================================

    private void addCandidate(File file, String category) {

        if (file == null || !file.exists()) {
            return;
        }

        String path = file.getAbsolutePath();

        if (candidatePaths.contains(path)) {
            return;
        }

        candidatePaths.add(path);

        candidates.add(
                new Candidate(file, category)
        );
    }

    // =========================================================
    // 8. ADD SKIPPED FILE
    // =========================================================

    private void addSkippedFile(File file, String reason) {

        skippedCount++;

        if (file == null || !file.exists() || !file.isFile()) {

            addSkippedInfo(
                    file == null
                            ? "Unknown file - " + reason
                            : file.getAbsolutePath() + " - " + reason
            );

            return;
        }

        String path = file.getAbsolutePath();

        if (skippedPaths.contains(path)) {
            return;
        }

        skippedPaths.add(path);

        skippedFiles.add(file);
        skippedReasons.add(reason);
    }

    // =========================================================
    // 9. ADD INFORMATIONAL SKIPPED ITEM
    // =========================================================

    private void addSkippedInfo(String message) {

        skippedCount++;

        if (skippedItems.size() < 500) {
            skippedItems.add(message);
        }
    }

    // =========================================================
    // 10. FIND EXACT DUPLICATE FILES
    // =========================================================

    private void findDuplicateFiles(File root) {

        Map<String, File> seenFiles = new HashMap<>();

        findDuplicatesRecursively(root, seenFiles);
    }

    private void findDuplicatesRecursively(
            File directory,
            Map<String, File> seenFiles) {

        if (directory == null || !directory.exists()) {
            return;
        }

        File[] files;

        try {

            files = directory.listFiles();

        } catch (SecurityException e) {

            return;
        }

        if (files == null) {
            return;
        }

        for (File file : files) {

            try {

                if (file.isDirectory()) {

                    if (!isProtectedDirectory(file)) {
                        findDuplicatesRecursively(file, seenFiles);
                    }

                } else if (file.isFile()) {

                    long size = file.length();

                    if (size <= 0 || size > MAX_DUPLICATE_CHECK_SIZE) {
                        continue;
                    }

                    if (candidatePaths.contains(file.getAbsolutePath())) {
                        continue;
                    }

                    String hash = size + "_" + calculateSHA256(file);

                    if (seenFiles.containsKey(hash)) {

                        addCandidate(
                                file,
                                "Exact duplicate file"
                        );

                    } else {

                        seenFiles.put(hash, file);
                    }
                }

            } catch (SecurityException e) {

                addSkippedFile(
                        file,
                        "Permission denied during duplicate check"
                );

            } catch (Exception e) {

                addSkippedFile(
                        file,
                        "Duplicate check failed"
                );
            }
        }
    }

    // =========================================================
    // 11. CALCULATE SHA-256
    // =========================================================

    private String calculateSHA256(File file) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        try (FileInputStream input =
                     new FileInputStream(file)) {

            byte[] buffer = new byte[8192];

            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {

                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();

        StringBuilder result = new StringBuilder();

        for (byte b : hashBytes) {

            result.append(
                    String.format(Locale.US, "%02x", b)
            );
        }

        return result.toString();
    }

    // =========================================================
    // 12. REVIEW RECOMMENDED FILES
    // =========================================================

    private void showReviewDialog() {

        if (activity.isFinishing()) {
            return;
        }

        String[] labels = new String[candidates.size()];

        boolean[] checked = new boolean[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {

            Candidate item = candidates.get(i);

            labels[i] =
                    item.file.getName()
                            + "\nCategory: " + item.category
                            + "\nSize: " + formatSize(item.file.length())
                            + "\nPath: " + item.file.getAbsolutePath();

            // Selected by default, user can uncheck.
            checked[i] = true;
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(
                        "Recommended Files: " + candidates.size()
                                + "\nUncheck files you want to keep"
                )
                .setMultiChoiceItems(
                        labels,
                        checked,
                        (d, which, isChecked) -> {
                            checked[which] = isChecked;
                        }
                )
                .setPositiveButton("Delete Selected", null)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Unmark All", null)
                .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(button -> {

                        ArrayList<File> selectedFiles =
                                new ArrayList<>();

                        for (int i = 0; i < checked.length; i++) {

                            if (checked[i]) {

                                File file = candidates.get(i).file;

                                if (file.exists()) {
                                    selectedFiles.add(file);
                                }
                            }
                        }

                        if (selectedFiles.isEmpty()) {

                            Toast.makeText(
                                    activity,
                                    "No files selected",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        dialog.dismiss();

                        confirmDeletion(selectedFiles);
                    });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(button -> {

                        for (int i = 0; i < checked.length; i++) {

                            checked[i] = false;

                            dialog.getListView()
                                    .setItemChecked(i, false);
                        }

                        Toast.makeText(
                                activity,
                                "All files unmarked",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        });

        dialog.show();
    }

    // =========================================================
    // 13. CONFIRM DELETION
    // =========================================================

    private void confirmDeletion(ArrayList<File> selectedFiles) {

        new AlertDialog.Builder(activity)
                .setTitle("Confirm Deletion")
                .setMessage(
                        "Selected files: " + selectedFiles.size()
                                + "\n\nThese files will be permanently deleted. "
                                + "Please check that you have kept all important files."
                )
                .setPositiveButton("Confirm Delete", (dialog, which) -> {

                    deleteSelectedFiles(selectedFiles);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {

                    showReviewDialog();
                })
                .show();
    }

    // =========================================================
    // 14. DELETE SELECTED FILES
    // =========================================================

    private void deleteSelectedFiles(ArrayList<File> selectedFiles) {

        Toast.makeText(
                activity,
                "Deleting selected files...",
                Toast.LENGTH_SHORT
        ).show();

        executor.execute(() -> {

            int success = 0;
            long freed = 0;

            for (File file : selectedFiles) {

                if (file == null || !file.exists()) {
                    continue;
                }

                try {

                    long size = file.length();

                    if (file.delete()) {

                        success++;
                        freed += size;

                        failedPaths.remove(file.getAbsolutePath());
                        removeFromFailedFiles(file);

                    } else {

                        addFailedFile(file);
                    }

                } catch (Exception e) {

                    addFailedFile(file);
                }
            }

            final int deleted = success;
            final long totalFreed = freed;

            mainHandler.post(() -> {

                if (activity.isFinishing()) {
                    return;
                }

                showFinalReport(deleted, totalFreed);
            });
        });
    }

    // =========================================================
    // 15. ADD FAILED FILE
    // =========================================================

    private void addFailedFile(File file) {

        if (file == null) {
            return;
        }

        String path = file.getAbsolutePath();

        if (failedPaths.contains(path)) {
            return;
        }

        failedPaths.add(path);
        failedFiles.add(file);
    }

    // =========================================================
    // 16. REMOVE FAILED FILE
    // =========================================================

    private void removeFromFailedFiles(File file) {

        if (file == null) {
            return;
        }

        String path = file.getAbsolutePath();

        failedFiles.removeIf(
                item -> item.getAbsolutePath().equals(path)
        );
    }

    // =========================================================
    // 17. FINAL CLEANUP REPORT
    // =========================================================

    private void showFinalReport(int deleted, long freed) {

        String message =
                "Cleanup completed!\n\n"
                        + "Successfully deleted: " + deleted + "\n"
                        + "Storage freed: " + formatSize(freed) + "\n"
                        + "Deletion failed: " + failedFiles.size() + "\n"
                        + "Skipped files: " + skippedFiles.size() + "\n"
                        + "Skipped folders/items: " + skippedItems.size();

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Cleanup Report")
                .setMessage(message)
                .setPositiveButton("Close", null)
                .setNegativeButton("Review Skipped", null)
                .setNeutralButton("View Failed", null)
                .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setOnClickListener(button -> {

                        dialog.dismiss();
                        showSkippedFilesDialog();
                    });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(button -> {

                        dialog.dismiss();
                        showFailedFilesDialog();
                    });
        });

        dialog.show();
    }

    // =========================================================
    // 18. DISPLAY SKIPPED FILES WITH CHECKBOXES
    // =========================================================

    private void showSkippedFilesDialog() {

        ArrayList<File> availableFiles = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < skippedFiles.size(); i++) {

            File file = skippedFiles.get(i);

            if (file != null && file.exists() && file.isFile()) {

                availableFiles.add(file);

                labels.add(
                        file.getName()
                                + "\nReason: " + skippedReasons.get(i)
                                + "\nSize: " + formatSize(file.length())
                                + "\nPath: " + file.getAbsolutePath()
                );
            }
        }

        if (availableFiles.isEmpty()) {

            showSkippedInformationDialog();
            return;
        }

        String[] items = labels.toArray(new String[0]);

        // Skipped files are NOT selected by default.
        boolean[] checked = new boolean[availableFiles.size()];

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(
                        "Skipped Files: " + availableFiles.size()
                                + "\nCheck files you want to retry"
                )
                .setMultiChoiceItems(
                        items,
                        checked,
                        (d, which, isChecked) -> {
                            checked[which] = isChecked;
                        }
                )
                .setPositiveButton("Retry Selected", null)
                .setNegativeButton("Close", null)
                .setNeutralButton("View Protected Items", null)
                .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(button -> {

                        ArrayList<File> selected = new ArrayList<>();

                        for (int i = 0; i < checked.length; i++) {

                            if (checked[i]) {
                                selected.add(availableFiles.get(i));
                            }
                        }

                        if (selected.isEmpty()) {

                            Toast.makeText(
                                    activity,
                                    "Please check at least one file",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        dialog.dismiss();

                        confirmSkippedDeletion(selected);
                    });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(button -> {

                        dialog.dismiss();
                        showSkippedInformationDialog();
                    });
        });

        dialog.show();
    }

    // =========================================================
    // 19. CONFIRM SKIPPED FILE DELETION
    // =========================================================

    private void confirmSkippedDeletion(ArrayList<File> selected) {

        new AlertDialog.Builder(activity)
                .setTitle("Retry Deletion")
                .setMessage(
                        "You selected " + selected.size()
                                + " skipped file(s).\n\n"
                                + "The app will attempt to delete these files. "
                                + "Android may still block some deletions."
                )
                .setPositiveButton("Retry Delete", (dialog, which) -> {

                    retrySelectedFiles(selected);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {

                    showSkippedFilesDialog();
                })
                .show();
    }

    // =========================================================
    // 20. DISPLAY PROTECTED FOLDERS AND OTHER SKIPPED ITEMS
    // =========================================================

    private void showSkippedInformationDialog() {

        if (skippedItems.isEmpty()) {

            new AlertDialog.Builder(activity)
                    .setTitle("Protected Items")
                    .setMessage("No additional skipped folders or items were recorded.")
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        StringBuilder message = new StringBuilder();

        for (int i = 0; i < skippedItems.size(); i++) {

            message.append(i + 1)
                    .append(". ")
                    .append(skippedItems.get(i))
                    .append("\n\n");
        }

        new AlertDialog.Builder(activity)
                .setTitle("Protected / Inaccessible Items")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // =========================================================
    // 21. DISPLAY FAILED FILES WITH CHECKBOXES
    // =========================================================

    private void showFailedFilesDialog() {

        ArrayList<File> availableFiles = new ArrayList<>();

        ArrayList<String> labels = new ArrayList<>();

        for (File file : failedFiles) {

            if (file != null && file.exists()) {

                availableFiles.add(file);

                labels.add(
                        file.getName()
                                + "\nStatus: Delete failed"
                                + "\nSize: " + formatSize(file.length())
                                + "\nPath: " + file.getAbsolutePath()
                );
            }
        }

        if (availableFiles.isEmpty()) {

            new AlertDialog.Builder(activity)
                    .setTitle("Failed Files")
                    .setMessage("There are no remaining failed files.")
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        String[] items = labels.toArray(new String[0]);

        boolean[] checked = new boolean[availableFiles.size()];

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(
                        "Failed Files: " + availableFiles.size()
                                + "\nCheck files you want to retry"
                )
                .setMultiChoiceItems(
                        items,
                        checked,
                        (d, which, isChecked) -> {
                            checked[which] = isChecked;
                        }
                )
                .setPositiveButton("Retry Selected", null)
                .setNegativeButton("Close", null)
                .create();

        dialog.setOnShowListener(v -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(button -> {

                        ArrayList<File> selected = new ArrayList<>();

                        for (int i = 0; i < checked.length; i++) {

                            if (checked[i]) {
                                selected.add(availableFiles.get(i));
                            }
                        }

                        if (selected.isEmpty()) {

                            Toast.makeText(
                                    activity,
                                    "Please select files to retry",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        dialog.dismiss();

                        retrySelectedFiles(selected);
                    });
        });

        dialog.show();
    }

    // =========================================================
    // 22. RETRY SELECTED FILES
    // =========================================================

    private void retrySelectedFiles(ArrayList<File> selectedFiles) {

        Toast.makeText(
                activity,
                "Retrying selected files...",
                Toast.LENGTH_SHORT
        ).show();

        executor.execute(() -> {

            int success = 0;
            long freed = 0;

            for (File file : selectedFiles) {

                if (file == null || !file.exists()) {
                    continue;
                }

                try {

                    long size = file.length();

                    if (file.delete()) {

                        success++;
                        freed += size;

                        removeFromFailedFiles(file);
                        removeFromSkippedFiles(file);

                    } else {

                        addFailedFile(file);
                    }

                } catch (Exception e) {

                    addFailedFile(file);
                }
            }

            final int deleted = success;
            final long totalFreed = freed;
            final int remaining = countExistingFailedFiles();

            mainHandler.post(() -> {

                if (activity.isFinishing()) {
                    return;
                }

                new AlertDialog.Builder(activity)
                        .setTitle("Retry Result")
                        .setMessage(
                                "Successfully deleted: " + deleted
                                        + "\nStill failed: " + remaining
                                        + "\nStorage freed: " + formatSize(totalFreed)
                        )
                        .setPositiveButton("Close", null)
                        .setNeutralButton("View Remaining", (dialog, which) -> {

                            showFailedFilesDialog();
                        })
                        .show();
            });
        });
    }

    // =========================================================
    // 23. REMOVE SUCCESSFULLY DELETED FILE FROM SKIPPED LIST
    // =========================================================

    private void removeFromSkippedFiles(File file) {

        if (file == null) {
            return;
        }

        String path = file.getAbsolutePath();

        for (int i = skippedFiles.size() - 1; i >= 0; i--) {

            if (skippedFiles.get(i).getAbsolutePath().equals(path)) {

                skippedFiles.remove(i);
                skippedReasons.remove(i);
            }
        }

        skippedPaths.remove(path);
    }

    // =========================================================
    // 24. COUNT REMAINING FAILED FILES
    // =========================================================

    private int countExistingFailedFiles() {

        int count = 0;

        for (File file : failedFiles) {

            if (file != null && file.exists()) {
                count++;
            }
        }

        return count;
    }

    // =========================================================
    // 25. FORMAT FILE SIZE
    // =========================================================

    private String formatSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024) {

            return String.format(
                    Locale.US,
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024 * 1024) {

            return String.format(
                    Locale.US,
                    "%.2f MB",
                    bytes / (1024.0 * 1024)
            );
        }

        return String.format(
                Locale.US,
                "%.2f GB",
                bytes / (1024.0 * 1024 * 1024)
        );
    }

    // =========================================================
    // 26. CANDIDATE MODEL
    // =========================================================

    private static class Candidate {
        final File file;
        final String category;

        Candidate(File file, String category) {

            this.file = file;
            this.category = category;
        }
    }
}
