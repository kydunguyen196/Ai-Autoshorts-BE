Place a default background video at:

- `assets/backgrounds/default.mp4`

If the file is missing, the backend falls back to a generated solid-color background through FFmpeg.

You can generate a simple local default file with:

```bash
ffmpeg -y -f lavfi -i "color=c=0x202020:s=1080x1920:r=30" -t 60 -c:v libx264 -pix_fmt yuv420p assets/backgrounds/default.mp4
```
