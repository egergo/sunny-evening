package com.egergo.ncskeleton;

public class ColorSpaces {

    public static int yuvToArgbFast(int y, int u, int v) {
        int g = y + (77 * u + 25 * v) / 256;
        int r = y + u;
        int b = y + v;

        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static int yuvToArgbExact(int y, int u, int v) {
        int r = y + (int) (1.402f * u);
        int g = y - (int) (0.344f * v + 0.714f * u);
        int b = y + (int) (1.772f * v);

        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
    
    public static void yv12ToArgb(byte[] input, int[] output, int width, int height) {        
        int yfield = width * height;
        int halfwidth = width / 2;
        int halfheight = height / 2;
        int ufield = halfwidth * halfheight;
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int y = (byte) input[(i * width + j)] & 0xFF;
                int v = ((byte) input[yfield + (i / 2) * halfwidth + j / 2] & 0xFF) - 128;
                int u = ((byte) input[yfield + ufield + (i / 2) * halfwidth + j / 2] & 0xFF) - 128;
                
                //output[i * width + j] = ColorSpaces.yuvToArgbFast(y, u, v);
                output[i * width + j] = ColorSpaces.yuvToArgbExact(y, u, v);
            }
        }
    }    
    
    public static void yv12RemoveStride(byte[] input, byte[] output, int width, int height) {
        int stride = width + width % 16;
        int halfstride = stride / 2;
        halfstride = halfstride + halfstride % 16;

        for (int y = 0; y < height; y++) {
            System.arraycopy(input, y * stride, output, y * width, width);
            System.arraycopy(input, height * stride + y * halfstride, output, height * width + y * (width / 2), width / 2);
        }
    }    

}
