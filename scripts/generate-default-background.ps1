param(
  [string]$Output = "assets/backgrounds/default.mp4",
  [int]$DurationSeconds = 60
)

ffmpeg -y -f lavfi -i "color=c=0x202020:s=1080x1920:r=30" -t $DurationSeconds -c:v libx264 -pix_fmt yuv420p $Output
