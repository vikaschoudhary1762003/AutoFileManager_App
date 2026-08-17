package com.example.autofilemanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DownloadFileAdaptor
        extends RecyclerView.Adapter<DownloadFileAdaptor.FileViewHolder> {

    private final Context context;
    private final List<FileItem> fileList;

    public DownloadFileAdaptor(
            Context context,
            List<FileItem> fileList
    ) {
        this.context = context;
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(context).inflate(
                R.layout.activity_download_file_item,
                parent,
                false
        );

        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FileViewHolder holder,
            int position
    ) {

        FileItem file = fileList.get(position);

        // ==========================================
        // REAL FILE NAME
        // ==========================================

        holder.fileName.setText(file.getName());


        // ==========================================
        // REAL FILE PATH
        // ==========================================

        holder.filePath.setText(
                file.getPath()
        );


        // ==========================================
        // REAL FILE SIZE
        // ==========================================

        holder.fileSize.setText(
                formatFileSize(file.getSize())
        );


        // ==========================================
        // REAL MODIFIED TIME
        // ==========================================

        holder.fileTime.setText(
                formatDate(file.getDateModified())
        );


        // ==========================================
        // FILE ICON
        // ==========================================

        holder.fileIcon.setText(
                getFileIcon(file.getName())
        );


        // ==========================================
        // MORE BUTTON
        // ==========================================

        holder.fileMoreButton.setOnClickListener(v -> {

            // We will add Open / Delete / Share here later.

        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }


    // =========================================================
    // FILE ICON
    // =========================================================

    private String getFileIcon(String name) {

        String fileName = name.toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") ||
                fileName.endsWith(".webp") ||
                fileName.endsWith(".gif")) {

            return "▧";
        }

        if (fileName.endsWith(".mp4") ||
                fileName.endsWith(".mkv") ||
                fileName.endsWith(".avi") ||
                fileName.endsWith(".mov") ||
                fileName.endsWith(".3gp")) {

            return "▶";
        }

        if (fileName.endsWith(".mp3") ||
                fileName.endsWith(".wav") ||
                fileName.endsWith(".m4a") ||
                fileName.endsWith(".aac") ||
                fileName.endsWith(".ogg")) {

            return "♫";
        }

        if (fileName.endsWith(".pdf")) {

            return "PDF";
        }

        if (fileName.endsWith(".doc") ||
                fileName.endsWith(".docx")) {

            return "DOC";
        }

        if (fileName.endsWith(".xls") ||
                fileName.endsWith(".xlsx")) {

            return "XLS";
        }

        if (fileName.endsWith(".ppt") ||
                fileName.endsWith(".pptx")) {

            return "PPT";
        }

        if (fileName.endsWith(".apk")) {

            return "APK";
        }

        return "📄";
    }


    // =========================================================
    // FILE SIZE
    // =========================================================

    private String formatFileSize(long size) {

        if (size < 1024) {
            return size + " B";
        }

        if (size < 1024L * 1024L) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f KB",
                    size / 1024.0
            );
        }

        if (size < 1024L * 1024L * 1024L) {

            return String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    size / (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.2f GB",
                size / (1024.0 * 1024.0 * 1024.0)
        );
    }


    // =========================================================
    // DATE / TIME
    // =========================================================

    private String formatDate(long dateModified) {

        Date date = new Date(dateModified * 1000L);

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                );

        return formatter.format(date);
    }


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    static class FileViewHolder
            extends RecyclerView.ViewHolder {

        TextView fileIcon;
        TextView fileName;
        TextView filePath;
        TextView fileSize;
        TextView fileTime;
        TextView fileMoreButton;

        public FileViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            fileIcon = itemView.findViewById(
                    R.id.fileIcon
            );

            fileName = itemView.findViewById(
                    R.id.fileName
            );

            filePath = itemView.findViewById(
                    R.id.filePath
            );

            fileSize = itemView.findViewById(
                    R.id.fileSize
            );

            fileTime = itemView.findViewById(
                    R.id.fileTime
            );

            fileMoreButton = itemView.findViewById(
                    R.id.fileMoreButton
            );
        }
    }
}