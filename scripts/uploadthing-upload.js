/**
 * UploadThing Upload Script
 *
 * Usage: node uploadthing-upload.js <filePath> <field> <fileType> <originalFilename>
 * Env:   UPLOADTHING_TOKEN
 *
 * Outputs a single JSON line to stdout:
 *   { success, url, fileKey, fileName, fileSize, error }
 */

import { UTApi } from "uploadthing/server";
import fs from "node:fs";
import path from "node:path";

const [, , filePath, field, fileType, originalFilename] = process.argv;

if (!filePath) {
  console.log(JSON.stringify({ success: false, error: "filePath argument is required" }));
  process.exit(1);
}

if (!process.env.UPLOADTHING_TOKEN) {
  console.log(JSON.stringify({ success: false, error: "UPLOADTHING_TOKEN env var is not set" }));
  process.exit(1);
}

try {
  const utapi = new UTApi({ token: process.env.UPLOADTHING_TOKEN });

  const fileBuffer = fs.readFileSync(filePath);
  const name = originalFilename || path.basename(filePath);

  const mimeMap = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".gif": "image/gif",
    ".webp": "image/webp",
  };
  const ext = path.extname(name).toLowerCase();
  const contentType = mimeMap[ext] || "application/octet-stream";

  const file = new File([fileBuffer], name, { type: contentType });

  const response = await utapi.uploadFiles(file);

  if (response.error) {
    console.log(JSON.stringify({
      success: false,
      error: response.error.message || JSON.stringify(response.error),
    }));
    process.exit(1);
  }

  const data = response.data;
  console.log(JSON.stringify({
    success: true,
    url: data.url,
    fileKey: data.key,
    fileName: data.name,
    fileSize: data.size,
    error: null,
  }));
  process.exit(0);

} catch (err) {
  console.log(JSON.stringify({
    success: false,
    error: err.message || String(err),
  }));
  process.exit(1);
}
