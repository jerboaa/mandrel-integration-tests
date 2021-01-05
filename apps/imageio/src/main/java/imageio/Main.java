/*
 * Copyright (c) 2021, Red Hat Inc. All rights reserved.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package imageio;

import org.jfree.svg.SVGGraphics2D;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;

/**
 * This is a small toy app to produce several formats of images,
 * play a bit with color spaces...
 *
 * @author Michal Karm Babacek <karm@redhat.com>
 */

// $ rm -rf src/main/resources/META-INF/* mytest* target
// $ mvn clean package
// $ java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar target/imageio.jar
// $ jar uf target/imageio.jar -C src/main/resources/ META-INF
// $ native-image -H:IncludeResources=Grace_M._Hopper.jp2,FreeMono.ttf,FreeSerif.ttf --no-fallback -jar target/imageio.jar target/imageio
// $ rm -rf mytest*
// $ ./target/imageio

/**
 * Expected results (sha256 hashes):
 * ec29663c7be2fbebd39be4fcea1ca18e81c2e0f31d92f73b3907c47c9f59aa1e  mytest.bmp
 * 2e8b712b9e3fe240b9fd1d9ccedd622a63344f94998b9cca6d86baad7bac8e81  mytest.gif
 * c427e9377c1ad6e787a5c3bb0096eeca68d4c917254f541aefa92e027c2263e8  mytest.jpg
 * 68638780950b392fb5f4fbd93d900d14446c55a91c52afb2adc0cbbeb84508f8  mytest.png
 * b802d1d7609bb8503e14c7d59e13e554470f892ea3db266b3aaa9fff49aacb77  mytest.svg
 * 7b879c98b55e70a414db6796b76ddcd9c353abb9998bfb2f6b3aa2c1fa6bc81a  mytest.tiff
 * 3d823e4b879c4a094da95d8eccf5ec5c6a413ec64696502d52d6c68b496d1a91  mytest_toC.png
 * 0bd36a1d079a805792ec69a54061e0234042daa199d1a3adcc4e7f95125cc9c1  mytest_toG.png
 * 1190ea971d2ac879988a43007219dd31bd0e26852e5cd3b04d5d88d768de7d07  mytest_toL.png
 * 17db14e726ec590c8834dfb68320d05b4558dac82602df2fa54e04bf95e2f708  mytest_toP.png
 * 4ce0872610f9865d39bfcfc237230a1e3c3a1903ad93b9d4f2522955ed4769dc  mytest_toS.png
 * d5a9cae953a5d6436def014115254c4d7268f042a97069c07942e1ef9193998b  mytest.wbmp
 */
public class Main {

    /**
     * Takes JPEG2000 file Grace_M._Hopper.jp2, converts colour spaces and spits out PNG files.
     *
     * @throws IOException
     */
    public static void paintGrace() throws IOException {
        // Let's paint Grace
        final BufferedImage img = ImageIO.read(Main.class.getResourceAsStream("/Grace_M._Hopper.jp2"));
        final Map<String, ColorConvertOp> conversions = Map.of(
                "toG", new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null),
                "toC", new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_CIEXYZ), null),
                "toL", new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), null),
                "toP", new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_PYCC), null),
                "toS", new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null));
        conversions.forEach((name, conversion) -> {
            try {
                ImageIO.write(conversion.filter(img, null), "png", new File("mytest_" + name + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Paints a groovy picture, exercising some draw methods
     * @throws IOException
     * @throws FontFormatException
     */
    public static void paintRectangles() throws IOException, FontFormatException {
        loadFonts();
        final BufferedImage img = createABGRTestImage(new Color[]{
                        Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK},
                100, 500);

        // Handles transparency
        ImageIO.write(img, "TIFF", new File("mytest.tiff"));
        ImageIO.write(img, "GIF", new File("mytest.gif"));
        ImageIO.write(img, "PNG", new File("mytest.png"));
        Files.writeString(Path.of("mytest.svg"), toSVG(img));

        // Doesn't handle transparency
        final BufferedImage imgBGR = new BufferedImage(img.getWidth(), img.getHeight(), TYPE_3BYTE_BGR);
        imgBGR.getGraphics().drawImage(img, 0, 0, null);
        ImageIO.write(imgBGR, "JPG", new File("mytest.jpg"));
        ImageIO.write(imgBGR, "BMP", new File("mytest.bmp"));

        // Doesn't handle colours, it's monochrome
        final BufferedImage imgBINARY = new BufferedImage(img.getWidth(), img.getHeight(), TYPE_BYTE_BINARY);
        imgBINARY.getGraphics().drawImage(img, 0, 0, null);
        ImageIO.write(imgBINARY, "WBMP", new File("mytest.wbmp"));
    }

    private static void loadFonts() throws IOException, FontFormatException {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        // Font source: https://ftp.gnu.org/gnu/freefont/
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Main.class.getResourceAsStream("/FreeMono.ttf")));
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Main.class.getResourceAsStream("/FreeSerif.ttf")));
    }

    private static BufferedImage createABGRTestImage(final Color[] colors, final int dx, final int h) {
        final BufferedImage img = new BufferedImage(dx * colors.length, h, TYPE_4BYTE_ABGR);
        final Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Main rectangle
        g.setColor(Color.PINK);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        // Small rectangles rotated
        final int wCenter = img.getWidth() / 2;
        final int hCenter = img.getHeight() / 2;
        final AffineTransform originalMatrix = g.getTransform();
        final AffineTransform af = AffineTransform.getRotateInstance(Math.toRadians(5), wCenter, hCenter);
        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(i * dx, 0, dx, h);
            g.transform(af);
        }
        g.setTransform(originalMatrix);

        // Transparent circle
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g.setColor(Color.MAGENTA);
        g.fillOval(0, 0, img.getWidth(), img.getHeight());
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Label, text
        g.setColor(Color.BLACK);
        g.setFont(new Font("FreeMono", Font.PLAIN, 15));
        g.drawString("Mandrel", 20, 20);
        g.setFont(new Font("FreeSerif", Font.PLAIN, 15));
        g.drawString("Mandrel", 20, 60);
        g.dispose();
        return img;
    }

    public static String toSVG(BufferedImage img) {
        final SVGGraphics2D g2 = new SVGGraphics2D(img.getWidth(), img.getHeight());
        g2.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
        return g2.getSVGElement(null, true, null, null, null);
    }

    public static void main(String[] args) throws IOException, FontFormatException {
        paintGrace();
        paintRectangles();
    }
}