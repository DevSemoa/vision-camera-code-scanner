package com.visioncameracodescanner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VisionCameraCodeScannerPlugin extends FrameProcessorPlugin {
  private BarcodeScanner barcodeScanner = null;
  private int barcodeScannerFormatsBitmap = -1;

  private static final String TAG = "VisionCameraCodeScanner";

  @Override
  public Object callback(ImageProxy frame, ReadableMap params) {
    createBarcodeInstance(params);

    @SuppressWarnings("UnsafeOptInUsageError")
    Image mediaImage = frame.getImage();
    if (mediaImage != null) {
      ArrayList<InputImage> imagesToProcess = new ArrayList<>();

      int rotationDegrees = frame.getImageInfo().getRotationDegrees();
      InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);
      imagesToProcess.add(image);

      if (params != null && params.hasKey("checkInverted") && params.getBoolean("checkInverted")) {
        Bitmap bitmap = null;
        try {
          bitmap = ImageConvertUtils.getInstance().getUpRightBitmap(image.getBitmapInternal());
          Bitmap invertedBitmap = invert(bitmap);
          InputImage invertedImage = InputImage.fromBitmap(invertedBitmap, 0);
          imagesToProcess.add(invertedImage);
        } catch (Exception e) {
          Log.e(TAG, "Error processing inverted image: " + e.getMessage());
        }
      }

      try {
        ArrayList<Barcode> barcodes = new ArrayList<>();
        for (InputImage inputImage : imagesToProcess) {
          List<Barcode> result = barcodeScanner.process(inputImage);
          barcodes.addAll(result);
        }

        WritableArray array = convertBarcodesToWritableArray(barcodes);
        return array;
      } catch (Exception e) {
        Log.e(TAG, "Error processing barcodes: " + e.getMessage());
      } finally {
        // Close the mediaImage to avoid resource leaks
        mediaImage.close();
      }
    }
    return null;
  }

  private void createBarcodeInstance(ReadableMap params) {
    if (params != null && params.hasKey("formats")) {
      ReadableArray formatsArray = params.getArray("formats");
      if (formatsArray != null) {
        int[] formats = new int[formatsArray.size()];
        for (int i = 0; i < formatsArray.size(); i++) {
          formats[i] = formatsArray.getInt(i);
        }

        int formatsBitmap = 0;
        for (int format : formats) {
          formatsBitmap |= format;
        }

        if (barcodeScanner == null || formatsBitmap != barcodeScannerFormatsBitmap) {
          barcodeScanner = BarcodeScanning.getClient(
            new BarcodeScannerOptions.Builder()
              .setBarcodeFormats(formatsBitmap)
              .build()
          );
          barcodeScannerFormatsBitmap = formatsBitmap;
        }
      }
    }
  }

  private WritableArray convertBarcodesToWritableArray(List<Barcode> barcodes) {
    WritableArray array = Arguments.createArray();
    for (Barcode barcode : barcodes) {
      WritableMap map = convertBarcodeToWritableMap(barcode);
      array.pushMap(map);
    }
    return array;
  }

  private WritableMap convertBarcodeToWritableMap(Barcode barcode) {
    WritableMap map = Arguments.createMap();

    Rect boundingBox = barcode.getBoundingBox();
    if (boundingBox != null) {
      WritableMap boundingBoxMap = convertRectToWritableMap(boundingBox);
      map.putMap("boundingBox", boundingBoxMap);
    }

    Point[] cornerPoints = barcode.getCornerPoints();
    if (cornerPoints != null) {
      WritableArray cornerPointsArray = convertPointsToWritableArray(cornerPoints);
      map.putArray("cornerPoints", cornerPointsArray);
    }

    String displayValue = barcode.getDisplayValue();
    if (displayValue != null) {
      map.putString("displayValue", displayValue);
    }

    String rawValue = barcode.getRawValue();
    if (rawValue != null) {
      map.putString("rawValue", rawValue);
    }

    map.putInt("format", barcode.getFormat());
    map.putInt("valueType", barcode.getValueType());

    // Handle different barcode value types here and add to the map as needed

    return map;
  }

  private WritableMap convertRectToWritableMap(Rect rect) {
    WritableMap map = Arguments.createMap();
    map.putInt("top", rect.top);
    map.putInt("bottom", rect.bottom);
    map.putInt("left", rect.left);
    map.putInt("right", rect.right);
    return map;
  }

  private WritableArray convertPointsToWritableArray(Point[] points) {
    WritableArray array = Arguments.createArray();
    for (Point point : points) {
      WritableMap map = Arguments.createMap();
      map.putInt("x", point.x);
      map.putInt("y", point.y);
      array.pushMap(map);
    }
    return array;
  }

  // Bitmap Inversion
  private Bitmap invert(Bitmap src) {
    // Your invert method implementation remains unchanged
  }
}
