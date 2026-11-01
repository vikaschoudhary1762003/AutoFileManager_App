package com.example.autofilemanager;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

public class DocumentFetchure {

    public interface DocumentFetchureListner {
        void onDocumentsFetched(ArrayList<DocumentModel> docList);
    }

    public static void getAllDocumentFiles(
            Context context,
            DocumentFetchureListner listener
    ) {

        new Thread(() -> {

            ArrayList<DocumentModel> docList = new ArrayList<>();

            ContentResolver contentResolver =
                    context.getContentResolver();

            Uri collectionUri;

            /*
             * Android 10+
             */
            if (android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.Q) {

                collectionUri = MediaStore.Files.getContentUri(
                        MediaStore.VOLUME_EXTERNAL
                );

            } else {

                collectionUri = MediaStore.Files.getContentUri(
                        "external"
                );
            }


            /*
             * Columns we need
             */
            String[] projection = {

                    MediaStore.Files.FileColumns._ID,

                    MediaStore.Files.FileColumns.DISPLAY_NAME,

                    MediaStore.Files.FileColumns.SIZE,

                    MediaStore.Files.FileColumns.MIME_TYPE
            };


            /*
             * Document MIME types
             */
            String[] mimeTypes = {

                    // PDF
                    "application/pdf",

                    // DOC
                    "application/msword",

                    // DOCX
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

                    // XLS
                    "application/vnd.ms-excel",

                    // XLSX
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",

                    // PPT
                    "application/vnd.ms-powerpoint",

                    // PPTX
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",

                    // TXT
                    "text/plain",

                    // CSV
                    "text/csv",

                    // RTF
                    "application/rtf"
            };


            /*
             * Build:
             *
             * MIME_TYPE IN (?, ?, ?, ...)
             */
            StringBuilder selectionBuilder =
                    new StringBuilder();

            selectionBuilder.append(
                    MediaStore.Files.FileColumns.MIME_TYPE
            );

            selectionBuilder.append(" IN (");

            for (int i = 0; i < mimeTypes.length; i++) {

                selectionBuilder.append("?");

                if (i < mimeTypes.length - 1) {
                    selectionBuilder.append(",");
                }
            }

            selectionBuilder.append(")");


            String selection =
                    selectionBuilder.toString();


            /*
             * Newest documents first
             */
            String sortOrder =
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                            + " DESC";


            Cursor cursor = null;

            try {

                cursor = contentResolver.query(
                        collectionUri,
                        projection,
                        selection,
                        mimeTypes,
                        sortOrder
                );


                if (cursor != null) {

                    int idColumn =
                            cursor.getColumnIndex(
                                    MediaStore.Files.FileColumns._ID
                            );

                    int nameColumn =
                            cursor.getColumnIndex(
                                    MediaStore.Files.FileColumns.DISPLAY_NAME
                            );

                    int sizeColumn =
                            cursor.getColumnIndex(
                                    MediaStore.Files.FileColumns.SIZE
                            );

                    int mimeColumn =
                            cursor.getColumnIndex(
                                    MediaStore.Files.FileColumns.MIME_TYPE
                            );


                    while (cursor.moveToNext()) {

                        long id =
                                cursor.getLong(idColumn);


                        String name =
                                cursor.getString(nameColumn);


                        long size =
                                cursor.getLong(sizeColumn);


                        String mimeType =
                                cursor.getString(mimeColumn);


                        if (name == null ||
                                name.trim().isEmpty()) {

                            continue;
                        }


                        /*
                         * Create Content URI
                         */
                        Uri fileUri =
                                Uri.withAppendedPath(
                                        collectionUri,
                                        String.valueOf(id)
                                );


                        /*
                         * We don't need DATA/path.
                         *
                         * Modern Android uses content URI.
                         */
                        String path = "";


                        DocumentModel document =
                                new DocumentModel(
                                        name,
                                        path,
                                        fileUri,
                                        size,
                                        mimeType
                                );


                        docList.add(document);
                    }
                }

            } catch (Exception e) {

                /*
                 * VERY IMPORTANT:
                 * Show the actual error in Logcat.
                 */
                e.printStackTrace();

                android.util.Log.e(
                        "DOCUMENT_FETCHER",
                        "Error while fetching documents",
                        e
                );

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }


            /*
             * Send result to UI thread
             */
            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).post(() -> {

                listener.onDocumentsFetched(docList);

            });

        }).start();
    }
}