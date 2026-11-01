package com.example.autofilemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class DocumentFiles extends AppCompatActivity {

    //VARIABLE
    TextView backbtn ;
    private LinearLayout documentFileContainer;
    private TextView documentCountText;
    private TextView documentTotalSizeText;
    private TextView documentFileCountBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_document_files);

        //CONNECT TO ID
        backbtn = findViewById(R.id.BackButton);
        documentFileContainer = findViewById(R.id.documentFileContainer);
        documentCountText = findViewById(R.id.documentCount);
        documentTotalSizeText = findViewById(R.id.documentTotalSize);
        documentFileCountBottom = findViewById(R.id.documentFileCountBottom);

        //BACK TO HOME DASHBOARD
        backbtn.setOnClickListener(view->{
            Intent intent = new Intent(DocumentFiles.this , MainActivity.class);
            startActivity(intent);
            finish();
        });


        //FUNCTION/ LOGIC TO FETCH ALL DOCUMENT FILES FROM MOBILE AND DISPLAY IN DOCUMENT PAGE
        loadAllDeviceDocuments();
    }

    private void loadAllDeviceDocuments() {
        Toast.makeText(this, "Scanning documents...", Toast.LENGTH_SHORT).show();

        // Query the custom background engine tool you built
        DocumentFetchure.getAllDocumentFiles(this, new DocumentFetchure.DocumentFetchureListner() {
            @Override
            public void onDocumentsFetched(ArrayList<DocumentModel> docList) {
                // Clear out mock visual template layout container rows
                documentFileContainer.removeAllViews();

                if (docList.isEmpty()) {
                    Toast.makeText(DocumentFiles.this, "No valid documents found.", Toast.LENGTH_SHORT).show();
                    documentCountText.setText("0 Files");
                    documentFileCountBottom.setText("0 files");
                    documentTotalSizeText.setText("0 B");
                    return;
                }

                long totalCalculatedSize = 0;

                // Loop and create views purely in Java (No 3rd file layout needed!)
                for (DocumentModel doc : docList) {
                    totalCalculatedSize += doc.getSize();

                    // 1. Create the Main Row Container (Horizontal Layout)
                    LinearLayout rowLayout = new LinearLayout(DocumentFiles.this);
                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowLayout.setLayoutParams(rowParams);
                    rowLayout.setPadding(0, 30, 0, 30); // Adds top/bottom padding for the row item
                    rowLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    rowLayout.setClickable(true);
                    rowLayout.setFocusable(true);

                    // 2. Create the File Extension Type Badge (Square placeholder like your summary block)
                    TextView typeBadge = new TextView(DocumentFiles.this);
                    LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(130, 130);
                    badgeParams.setMargins(0, 0, 35, 0); // Adds margins to the right side of the badge
                    typeBadge.setLayoutParams(badgeParams);
                    typeBadge.setGravity(android.view.Gravity.CENTER);
                    typeBadge.setTextSize(12);
                    typeBadge.setTypeface(null, android.graphics.Typeface.BOLD);

                    // Style badge backgrounds & text color accent configurations
                    String lowerName = doc.getName().toLowerCase();
                    if (lowerName.endsWith(".pdf")) {
                        typeBadge.setText("PDF");
                        typeBadge.setBackgroundColor(0xFFFFE0E0);
                        typeBadge.setTextColor(0xFFE53935);
                    } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
                        typeBadge.setText("WORD");
                        typeBadge.setBackgroundColor(0xFFE3F2FD);
                        typeBadge.setTextColor(0xFF1E88E5);
                    } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
                        typeBadge.setText("EXCEL");
                        typeBadge.setBackgroundColor(0xFFE8F5E9);
                        typeBadge.setTextColor(0xFF43A047);
                    } else if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
                        typeBadge.setText("PPT");
                        typeBadge.setBackgroundColor(0xFFFFF3E0);
                        typeBadge.setTextColor(0xFFF4511E);
                    } else {
                        typeBadge.setText("DOC");
                        typeBadge.setBackgroundColor(0xFFF5F5F5);
                        typeBadge.setTextColor(0xFF616161);
                    }

                    // 3. Create Inner Text Layout Container (Vertical Layout)
                    LinearLayout textLayout = new LinearLayout(DocumentFiles.this);
                    textLayout.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f); // weight = 1 to fill space
                    textLayout.setLayoutParams(textParams);

                    // 4. Create File Name TextView
                    TextView fileNameText = new TextView(DocumentFiles.this);
                    fileNameText.setText(doc.getName());
                    fileNameText.setTextColor(0xFF171717);
                    fileNameText.setTextSize(15);
                    fileNameText.setTypeface(null, android.graphics.Typeface.BOLD);
                    fileNameText.setSingleLine(true);
                    fileNameText.setEllipsize(android.text.TextUtils.TruncateAt.END);

                    // 5. Create File Size TextView
                    TextView fileSizeText = new TextView(DocumentFiles.this);
                    fileSizeText.setText(formatFileSize(doc.getSize()));
                    fileSizeText.setTextColor(0xFF777777);
                    fileSizeText.setTextSize(13);
                    fileSizeText.setPadding(0, 4, 0, 0);

                    // Assemble texts inside the vertical text layout block
                    textLayout.addView(fileNameText);
                    textLayout.addView(fileSizeText);

                    // Assemble badge and text layouts inside the main horizontal row layout item
                    rowLayout.addView(typeBadge);
                    rowLayout.addView(textLayout);

                    // Run file open logic when user clicks anywhere on the row layout container
                    rowLayout.setOnClickListener(v -> executeOpenFileIntent(doc));

                    // Direct injection straight into your layout view file's layout container tag
                    documentFileContainer.addView(rowLayout);

                    // 6. Create a Clean Divider Line Separator between file items
                    View lineDivider = new View(DocumentFiles.this);
                    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 2);
                    lineDivider.setLayoutParams(dividerParams);
                    lineDivider.setBackgroundColor(0xFFEEEEEE);

                    documentFileContainer.addView(lineDivider);
                }

                // Update summary metadata counts display layout widget strings elements text
                String fileCountValue = docList.size() + " Files";
                documentCountText.setText(fileCountValue);
                documentFileCountBottom.setText(fileCountValue.toLowerCase());
                documentTotalSizeText.setText(formatFileSize(totalCalculatedSize));
            }
        });
    }


    // Secondary Intent handling block to load openable resources in outside software viewer apps
    private void executeOpenFileIntent(DocumentModel model) {

        try {

            Uri contentUri = model.getUri();

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);

            viewIntent.setDataAndType(
                    contentUri,
                    model.getMimeType()
            );

            viewIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            viewIntent,
                            "Open your file using:"
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "No matching application found to open this document.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Helper math tool formatting bytes count to smooth user interface strings units
    private String formatFileSize(long bytesCount) {
        if (bytesCount <= 0) return "0 B";
        final String[] metricsArray = new String[]{"B", "KB", "MB", "GB"};
        int conversionIndex = (int) (Math.log10(bytesCount) / Math.log10(1024));
        return String.format(Locale.US, "%.1f %s", bytesCount / Math.pow(1024, conversionIndex), metricsArray[conversionIndex]);
    }
}