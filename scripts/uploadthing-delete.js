/**
 * UploadThing Delete Script
 *
 * Usage: node uploadthing-delete.js <fileKey1> [fileKey2] ...
 * Env:   UPLOADTHING_TOKEN
 *
 * Outputs a single JSON line to stdout:
 *   { success, deletedFiles, error }
 */

import { UTApi } from "uploadthing/server";

const fileKeys = process.argv.slice(2);

if (fileKeys.length === 0) {
  console.log(JSON.stringify({ success: false, deletedFiles: [], error: "No file keys provided" }));
  process.exit(1);
}

if (!process.env.UPLOADTHING_TOKEN) {
  console.log(JSON.stringify({ success: false, deletedFiles: [], error: "UPLOADTHING_TOKEN env var is not set" }));
  process.exit(1);
}

try {
  const utapi = new UTApi({ token: process.env.UPLOADTHING_TOKEN });

  const result = await utapi.deleteFiles(fileKeys);

  console.log(JSON.stringify({
    success: result.success,
    deletedFiles: result.success ? fileKeys : [],
    error: result.success ? null : "Delete operation returned unsuccessful",
  }));

  process.exit(result.success ? 0 : 1);

} catch (err) {
  console.log(JSON.stringify({
    success: false,
    deletedFiles: [],
    error: err.message || String(err),
  }));
  process.exit(1);
}
