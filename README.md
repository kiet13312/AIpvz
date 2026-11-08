# Garden Defense (bản cập nhật)

Dự án Android (Java, không cần thư viện ngoài). Build: `gradle :app:assembleDebug` hoặc dùng workflow GitHub Actions có sẵn (.github/workflows/build.yml).

## Cấu trúc mã nguồn
- `MainActivity.java` – Activity, âm thanh (Plant Food / BINU), video chiến thắng.
- `GameView.java.gz` – mã nguồn engine được nén; Gradle tự giải nén thành `GameView.java` trước khi biên dịch.
- `Art.java` – toàn bộ hình ảnh cây / đạn / nắng / máy cắt cỏ vẽ vector bằng Canvas (phong cách PvZ2, có animation).
- `old_assets/` – ảnh cũ đã được thay thế (không đóng vào APK).

## Tóm tắt game
- 10 cây: Sunflower, Peashooter, Giga-nut, Potato Mine, Chomper, Repeater, Cabbage-pult, Kernel-pult, Melon-pult, BINU.
- 200 map = 20 thế giới x 10 map (danh sách thế giới theo Wiki PvZ2). Map chia hết cho 5 là Boss (40 boss; từ map 10 boss triệu hồi thêm zombie). Thắng map 200 phát video kết thúc.
- Mỗi thế giới có màu nền/trang trí riêng và 1 cơ chế: bia mộ chắn đường, sương mù, bão cát, ô năng lượng (+50% tốc độ), ô vàng (tặng nắng), gió thổi lùi zombie.
- 13 mini-game: Mưa nắng, Bowling Giga-nut, Băng chuyền, Sinh tồn vô tận, Đấu Boss liên tiếp, Chạm diệt zombie, Đập bình, Mưa hạt giống, Phòng thủ cuối cùng, Một hàng sinh tử, Sương mù, Bãi mìn, Zombie tốc độ.
- Plant Food theo Wiki PvZ2:
  - Peashooter: 60 đạn dồn dập (~2s) | Repeater: hóa Gatling (4 đạn/lượt, mũ sắt, 4s)
  - Chomper: nuốt 3 zombie đầu hàng rồi ợ đẩy cả hàng về cột 9
  - Potato Mine: tự kích hoạt + sinh 2 mìn đã sẵn sàng
  - Cabbage-pult: bắp cải khổng lồ trúng mọi zombie | Kernel-pult: bơ trúng mọi zombie, choáng 8s | Melon-pult: dưa hấu khổng lồ nổ 3x3 trúng mọi zombie
  - Sunflower: tạo 150 nắng (3 cục) | Giga-nut: hồi đầy máu + vỏ giáp chịu 8000 sát thương (vẽ đè đúng khung ảnh Giga-nut, vỡ dần theo máu giáp).
- Giữ nguyên ảnh: Giganut, Gigapea, zombie, BINU (giganutplantfood.png không còn dùng).

## Admin Panel
Nút ADMIN ở màn hình chính (và trong menu Tạm dừng khi đang chơi): mở khóa toàn bộ map, mở khóa toàn bộ cây, +nắng, +xu, +Plant Food, cây bất tử, diệt hết zombie, qua màn ngay, xóa dữ liệu lưu.

## Tối ưu hiệu năng
- Ảnh PNG lớn (1536px...) được giải nén thu nhỏ và `inScaled=false` (trước đây bị phóng to theo density, rất tốn RAM).
- Nền bãi cỏ / menu vẽ sẵn 1 lần vào Bitmap; không tạo object mới trong vòng vẽ; chia nhỏ bước cập nhật để đạn không xuyên zombie khi x2 tốc độ.

## Sửa lỗi âm thanh Plant Food
Chỉ có 1 MediaPlayer Plant Food; tự dừng khi hết thời gian hiệu ứng, khi cây bị xẻng/bị ăn, khi tạm dừng, thoát màn hoặc thoát app.
