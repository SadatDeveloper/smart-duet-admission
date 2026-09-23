package com.example.smartduetadmissionsystem;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudinaryUploader {

    /*
     * IMPORTANT:
     * Put ONLY your Cloud Name and unsigned Upload Preset here.
     * NEVER put Cloudinary API Secret in the Android app.
     */
    private static final String CLOUD_NAME = "k3vl0ivf";
    private static final String UPLOAD_PRESET = "smart_duet_upload";

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String secureUrl);
        void onError(String message);
    }

    public CloudinaryUploader(Context context) {
        this.context = context.getApplicationContext();
    }

    public void upload(Uri fileUri, boolean isImage, UploadCallback callback) {
        if (fileUri == null) {
            callback.onError("File not selected.");
            return;
        }

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                String resourceType = isImage ? "image" : "auto";
                String endpoint = "https://api.cloudinary.com/v1_1/"
                        + CLOUD_NAME + "/" + resourceType + "/upload";

                String boundary = "----SmartDUET" + UUID.randomUUID();
                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                try (DataOutputStream output =
                             new DataOutputStream(connection.getOutputStream())) {

                    writeTextPart(output, boundary, "upload_preset", UPLOAD_PRESET);

                    String mimeType = context.getContentResolver().getType(fileUri);
                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }

                    String fileName = getFileName(fileUri);
                    writeFilePart(
                            output,
                            boundary,
                            "file",
                            fileName,
                            mimeType,
                            fileUri
                    );

                    output.writeBytes("--" + boundary + "--\r\n");
                    output.flush();
                }

                int responseCode = connection.getResponseCode();
                InputStream responseStream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                String response = readResponse(responseStream);

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject json = new JSONObject(response);
                    String secureUrl = json.optString("secure_url", "");

                    if (secureUrl.isEmpty()) {
                        postError(callback, "Cloudinary did not return a secure URL.");
                    } else {
                        postSuccess(callback, secureUrl);
                    }
                } else {
                    String message = "Cloudinary upload failed (" + responseCode + ")";
                    try {
                        JSONObject errorJson = new JSONObject(response);
                        JSONObject errorObject = errorJson.optJSONObject("error");
                        if (errorObject != null) {
                            String cloudinaryMessage =
                                    errorObject.optString("message", "");
                            if (!cloudinaryMessage.isEmpty()) {
                                message += ": " + cloudinaryMessage;
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    postError(callback, message);
                }

            } catch (Exception e) {
                postError(callback, e.getMessage() == null
                        ? "Upload failed."
                        : e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void writeTextPart(
            DataOutputStream output,
            String boundary,
            String fieldName,
            String value
    ) throws IOException {

        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes(
                "Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n"
        );
        output.writeBytes(value + "\r\n");
    }

    private void writeFilePart(
            DataOutputStream output,
            String boundary,
            String fieldName,
            String fileName,
            String mimeType,
            Uri fileUri
    ) throws IOException {

        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes(
                "Content-Disposition: form-data; name=\"" + fieldName
                        + "\"; filename=\"" + fileName + "\"\r\n"
        );
        output.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

        try (InputStream input =
                     new BufferedInputStream(
                             context.getContentResolver().openInputStream(fileUri))) {

            if (input == null) {
                throw new IOException("Unable to open selected file.");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        output.writeBytes("\r\n");
    }

    private String getFileName(Uri uri) {
        String name = uri.getLastPathSegment();

        if (name == null || name.isEmpty()) {
            name = "smart_duet_" + System.currentTimeMillis();
        }

        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");

        return name;
    }

    private String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

    private void postSuccess(UploadCallback callback, String url) {
        mainHandler.post(() -> callback.onSuccess(url));
    }

    private void postError(UploadCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
