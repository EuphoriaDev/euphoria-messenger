package ru.euphoria.messenger.util;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;


/**
 * A set of color-related utility methods, building upon those available in {@code Color}.
 *
 * @since 1.0
 */
public class ColorUtil {
    private static final int MIN_ALPHA_SEARCH_MAX_ITERATIONS = 10;
    private static final int MIN_ALPHA_SEARCH_PRECISION = 1;

    private ColorUtil() {
    }

    /**
     * Darkens color on 35%. a.k.a pressed color of button
     *
     * @param color the color to darken
     * @return a new color which is darken of specified color
     */
    public static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.75f;
        color = Color.HSVToColor(hsv);
        return color;
    }

    /**
     * Lighten color on 10%
     *
     * @param color the color to lighten
     * @return a new color which is lighten of specified color
     */
    public static int lightenColor(int color) {
        return lightenColor(color, 1.1f);
    }

    /**
     * Lighten color
     *
     * @param color the color to lighten
     * @return a new color which is lighten of specified color
     */
    public static int lightenColor(int color, float lightFactor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= lightFactor;
        return Color.HSVToColor(hsv);
    }

    /**
     * Saturate Color
     *
     * @param color  the color to saturate
     * @param factor the saturate factor
     * @return a new color which is saturated of specified color
     */
    public static int saturateColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] *= factor;
        return Color.HSVToColor(hsv);
    }

    /**
     * Lighten color on 100%, e.g. material from 50 to 100
     *
     * @param color the color to saturate
     * @return a new color which is saturated of specified color
     */
    public static int saturateColor(int color) {
        return saturateColor(color, 2f);
    }

    /**
     * Set alpha to color on 15%
     *
     * @param color the color to set alpha
     * @return a new color which is alpha of specified color
     */
    public static int alphaColor(int color) {
        return alphaColor(color, 0.85f);
    }

    /**
     * Set alpha to color
     *
     * @param color       the color to set alpha
     * @param alphaFactor the factor for alpha, range [0...1]
     * @return a new color which is alpha of specified color
     */
    public static int alphaColor(int color, float alphaFactor) {
        int alpha = Color.alpha(color);

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb((int) (alpha * alphaFactor), red, green, blue);
    }

    /**
     * Composite two potentially translucent colors over each other and returns the result.
     */
    public static int compositeColors(@ColorInt int foreground, @ColorInt int background) {
        int bgAlpha = Color.alpha(background);
        int fgAlpha = Color.alpha(foreground);
        int a = compositeAlpha(fgAlpha, bgAlpha);

        int r = compositeComponent(Color.red(foreground), fgAlpha,
                Color.red(background), bgAlpha, a);
        int g = compositeComponent(Color.green(foreground), fgAlpha,
                Color.green(background), bgAlpha, a);
        int b = compositeComponent(Color.blue(foreground), fgAlpha,
                Color.blue(background), bgAlpha, a);

        return Color.argb(a, r, g, b);
    }

    private static int compositeAlpha(int foregroundAlpha, int backgroundAlpha) {
        return 0xFF - (((0xFF - backgroundAlpha) * (0xFF - foregroundAlpha)) / 0xFF);
    }

    private static int compositeComponent(int fgC, int fgA, int bgC, int bgA, int a) {
        if (a == 0) return 0;
        return ((0xFF * fgC * fgA) + (bgC * bgA * (0xFF - fgA))) / (a * 0xFF);
    }

    /**
     * Returns the luminance of a color.
     * <p>
     * Formula defined
     * <a href="http://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef">here</a>.
     * </p>
     */
    @FloatRange(from = 0.0, to = 1.0)
    public static double calculateLuminance(@ColorInt int color) {
        double red = Color.red(color) / 255d;
        red = red < 0.03928 ? red / 12.92 : Math.pow((red + 0.055) / 1.055, 2.4);

        double green = Color.green(color) / 255d;
        green = green < 0.03928 ? green / 12.92 : Math.pow((green + 0.055) / 1.055, 2.4);

        double blue = Color.blue(color) / 255d;
        blue = blue < 0.03928 ? blue / 12.92 : Math.pow((blue + 0.055) / 1.055, 2.4);

        return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
    }

    /**
     * Returns the contrast ratio between {@code foreground} and {@code background}.
     * {@code background} must be opaque.
     * <p>
     * Formula defined
     * <a href="http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef">here</a>.
     */
    public static double calculateContrast(@ColorInt int foreground, @ColorInt int background) {
        if (Color.alpha(background) != 255) {
            throw new IllegalArgumentException("background can not be translucent: #"
                    + Integer.toHexString(background));
        }
        if (Color.alpha(foreground) < 255) {
            // If the foreground is translucent, composite the foreground over the background
            foreground = compositeColors(foreground, background);
        }

        final double luminance1 = calculateLuminance(foreground) + 0.05;
        final double luminance2 = calculateLuminance(background) + 0.05;

        // Now return the lighter luminance divided by the darker luminance
        return Math.max(luminance1, luminance2) / Math.min(luminance1, luminance2);
    }

    /**
     * Calculates the minimum alpha value which can be applied to {@code foreground} so that would
     * have a contrast value of at least {@code minContrastRatio} when compared to
     * {@code background}.
     *
     * @param foreground       the foreground color.
     * @param background       the background color. Should be opaque.
     * @param minContrastRatio the minimum contrast ratio.
     * @return the alpha value in the range 0-255, or -1 if no value could be calculated.
     */
    public static int calculateMinimumAlpha(@ColorInt int foreground, @ColorInt int background,
                                            float minContrastRatio) {
        if (Color.alpha(background) != 255) {
            throw new IllegalArgumentException("background can not be translucent: #"
                    + Integer.toHexString(background));
        }

        // First lets check that a fully opaque foreground has sufficient contrast
        int testForeground = setAlphaComponent(foreground, 255);
        double testRatio = calculateContrast(testForeground, background);
        if (testRatio < minContrastRatio) {
            // Fully opaque foreground does not have sufficient contrast, return error
            return -1;
        }

        // Binary search to find a value with the minimum value which provides sufficient contrast
        int numIterations = 0;
        int minAlpha = 0;
        int maxAlpha = 255;

        while (numIterations <= MIN_ALPHA_SEARCH_MAX_ITERATIONS &&
                (maxAlpha - minAlpha) > MIN_ALPHA_SEARCH_PRECISION) {
            final int testAlpha = (minAlpha + maxAlpha) / 2;

            testForeground = setAlphaComponent(foreground, testAlpha);
            testRatio = calculateContrast(testForeground, background);

            if (testRatio < minContrastRatio) {
                minAlpha = testAlpha;
            } else {
                maxAlpha = testAlpha;
            }

            numIterations++;
        }

        // Conservatively return the max of the range of possible alphas, which is known to pass.
        return maxAlpha;
    }

    /**
     * Convert RGB components to HSL (hue-saturation-lightness).
     * <ul>
     * <li>hsl[0] is Hue [0 .. 360)</li>
     * <li>hsl[1] is Saturation [0...1]</li>
     * <li>hsl[2] is Lightness [0...1]</li>
     * </ul>
     *
     * @param r   red component value [0..255]
     * @param g   green component value [0..255]
     * @param b   blue component value [0..255]
     * @param hsl 3 element array which holds the resulting HSL components.
     */
    public static void RGBToHSL(@IntRange(from = 0x0, to = 0xFF) int r,
                                @IntRange(from = 0x0, to = 0xFF) int g, @IntRange(from = 0x0, to = 0xFF) int b,
                                @NonNull float[] hsl) {
        final float rf = r / 255f;
        final float gf = g / 255f;
        final float bf = b / 255f;

        final float max = Math.max(rf, Math.max(gf, bf));
        final float min = Math.min(rf, Math.min(gf, bf));
        final float deltaMaxMin = max - min;

        float h, s;
        float l = (max + min) / 2f;

        if (max == min) {
            // Monochromatic
            h = s = 0f;
        } else {
            if (max == rf) {
                h = ((gf - bf) / deltaMaxMin) % 6f;
            } else if (max == gf) {
                h = ((bf - rf) / deltaMaxMin) + 2f;
            } else {
                h = ((rf - gf) / deltaMaxMin) + 4f;
            }

            s = deltaMaxMin / (1f - Math.abs(2f * l - 1f));
        }

        h = (h * 60f) % 360f;
        if (h < 0) {
            h += 360f;
        }

        hsl[0] = constrain(h, 0f, 360f);
        hsl[1] = constrain(s, 0f, 1f);
        hsl[2] = constrain(l, 0f, 1f);
    }

    /**
     * Convert the ARGB color to its HSL (hue-saturation-lightness) components.
     * <ul>
     * <li>hsl[0] is Hue [0 .. 360)</li>
     * <li>hsl[1] is Saturation [0...1]</li>
     * <li>hsl[2] is Lightness [0...1]</li>
     * </ul>
     *
     * @param color the ARGB color to convert. The alpha component is ignored.
     * @param hsl   3 element array which holds the resulting HSL components.
     */
    public static void colorToHSL(@ColorInt int color, @NonNull float[] hsl) {
        RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), hsl);
    }

    /**
     * Convert HSL (hue-saturation-lightness) components to a RGB color.
     * <ul>
     * <li>hsl[0] is Hue [0 .. 360)</li>
     * <li>hsl[1] is Saturation [0...1]</li>
     * <li>hsl[2] is Lightness [0...1]</li>
     * </ul>
     * If hsv values are out of range, they are pinned.
     *
     * @param hsl 3 element array which holds the input HSL components.
     * @return the resulting RGB color
     */
    @ColorInt
    public static int HSLToColor(@NonNull float[] hsl) {
        final float h = hsl[0];
        final float s = hsl[1];
        final float l = hsl[2];

        final float c = (1f - Math.abs(2 * l - 1f)) * s;
        final float m = l - 0.5f * c;
        final float x = c * (1f - Math.abs((h / 60f % 2f) - 1f));

        final int hueSegment = (int) h / 60;

        int r = 0, g = 0, b = 0;

        switch (hueSegment) {
            case 0:
                r = Math.round(255 * (c + m));
                g = Math.round(255 * (x + m));
                b = Math.round(255 * m);
                break;
            case 1:
                r = Math.round(255 * (x + m));
                g = Math.round(255 * (c + m));
                b = Math.round(255 * m);
                break;
            case 2:
                r = Math.round(255 * m);
                g = Math.round(255 * (c + m));
                b = Math.round(255 * (x + m));
                break;
            case 3:
                r = Math.round(255 * m);
                g = Math.round(255 * (x + m));
                b = Math.round(255 * (c + m));
                break;
            case 4:
                r = Math.round(255 * (x + m));
                g = Math.round(255 * m);
                b = Math.round(255 * (c + m));
                break;
            case 5:
            case 6:
                r = Math.round(255 * (c + m));
                g = Math.round(255 * m);
                b = Math.round(255 * (x + m));
                break;
        }

        r = constrain(r, 0, 255);
        g = constrain(g, 0, 255);
        b = constrain(b, 0, 255);

        return Color.rgb(r, g, b);
    }

    /**
     * Set the alpha component of {@code color} to be {@code alpha}.
     */
    @ColorInt
    public static int setAlphaComponent(@ColorInt int color,
                                        @IntRange(from = 0x0, to = 0xFF) int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("alpha must be between 0 and 255.");
        }
        return (color & 0x00ffffff) | (alpha << 24);
    }

    private static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    private static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

}
