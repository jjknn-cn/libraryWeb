package com.example.library.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

/**
 * 验证码 Servlet：生成 4 位字符图片，验证码保存在 Session 中。
 */
@WebServlet("/captcha")
public class CaptchaServlet extends HttpServlet {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, width, height);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(150, 210));
            g.setStroke(new BasicStroke(1f));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height), RANDOM.nextInt(width), RANDOM.nextInt(height));
        }

        StringBuilder code = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            char c = CHARS.charAt(RANDOM.nextInt(CHARS.length()));
            code.append(c);
            g.setColor(randomColor(20, 130));
            g.setFont(new Font("Arial", Font.BOLD, 26 + RANDOM.nextInt(4)));
            AffineTransform old = g.getTransform();
            double angle = (RANDOM.nextDouble() - 0.5) * 0.5;
            g.rotate(angle, 18 + i * 26, 28);
            g.drawString(String.valueOf(c), 12 + i * 26, 30);
            g.setTransform(old);
        }
        g.dispose();

        HttpSession session = req.getSession(true);
        session.setAttribute("captcha", code.toString());

        resp.setContentType("image/png");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        try (OutputStream out = resp.getOutputStream()) {
            ImageIO.write(image, "png", out);
            out.flush();
        }
    }

    private Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(min + RANDOM.nextInt(bound), min + RANDOM.nextInt(bound), min + RANDOM.nextInt(bound));
    }
}
