import { createCanvas, registerFont } from "canvas";
import fs from "node:fs";
import path from "node:path";

// Adapted from https://github.com/vandamd/light-template/blob/main/scripts/generate-icon.js
// for this Kotlin/Android project: app name comes from verses/lighttool.toml instead of an
// Expo app.json. Font file isn't committed (see .gitignore) — copy PublicSans-Regular.ttf
// into assets/fonts/ before running this.
const fontPath = path.join(
  import.meta.dirname,
  "../assets/fonts/PublicSans-Regular.ttf",
);
registerFont(fontPath, { family: "PublicSans" });

const lighttoolToml = fs.readFileSync(
  path.join(import.meta.dirname, "../verses/lighttool.toml"),
  "utf8",
);
const appName = lighttoolToml.match(/^label\s*=\s*"(.+)"$/m)[1];
const firstLetter = appName.charAt(0).toUpperCase();

const size = 100;
const canvas = createCanvas(size, size);
const ctx = canvas.getContext("2d");

ctx.fillStyle = "black";
ctx.fillRect(0, 0, size, size);

ctx.fillStyle = "white";
ctx.font = "85.4px PublicSans";
ctx.textAlign = "center";
ctx.textBaseline = "alphabetic";
const metrics = ctx.measureText(firstLetter);
const y =
  (size + metrics.actualBoundingBoxAscent - metrics.actualBoundingBoxDescent) /
  2;
ctx.fillText(firstLetter, size / 2, y);

const outputPath = path.join(import.meta.dirname, "../assets/images/icon.png");
const dir = path.dirname(outputPath);
if (!fs.existsSync(dir)) {
  fs.mkdirSync(dir, { recursive: true });
}

const buffer = canvas.toBuffer("image/png");
fs.writeFileSync(outputPath, buffer);

console.log(`Icon saved to ${outputPath}`);
