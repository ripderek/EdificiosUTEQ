package com.example.textdetection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.textdetection.ml.Model;
import com.example.textdetection.ml.Modelomascota;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import camerax.CameraConnectionFragment;
import camerax.ImageUtils;

public class MainActivity extends AppCompatActivity implements OnSuccessListener<Text>,
        OnFailureListener, ImageReader.OnImageAvailableListener {
    public static int REQUEST_CAMERA = 111;
    Bitmap mSelectedImage;
    ImageView mImageView;
    ArrayList<String> permisosNoAprobados;
    Button btnCamara;
    TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayList<String> permisos_requeridos = new ArrayList<String>();
        permisos_requeridos.add(Manifest.permission.CAMERA);
        permisos_requeridos.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        permisos_requeridos.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        //mImageView= findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);
        btnCamara = findViewById(R.id.btCamera);
        permisosNoAprobados  = getPermisosNoAprobados(permisos_requeridos);
        requestPermissions(permisosNoAprobados.toArray(new String[permisosNoAprobados.size()]),
                100);
    }

    public void abrirCamera (View view){
    //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    //startActivityForResult(intent, REQUEST_CAMERA);
    this.setFragment();
}
    @Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && null != data) {
        try {
            if (requestCode == REQUEST_CAMERA)
                mSelectedImage = (Bitmap) data.getExtras().get("data");
            else
                 mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

            mImageView.setImageBitmap(mSelectedImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(int i=0; i<permissions.length; i++){
            if(permissions[i].equals(Manifest.permission.CAMERA)){
                btnCamara.setEnabled(grantResults[i] == PackageManager.PERMISSION_GRANTED);
            } else if(permissions[i].equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE) ||
                    permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)
            ) {
                //btnGaleria.setEnabled(grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
        }
    }

    public ArrayList<String> getPermisosNoAprobados(ArrayList<String>  listaPermisos) {
        ArrayList<String> list = new ArrayList<String>();
        Boolean habilitado;


        if (Build.VERSION.SDK_INT >= 23)
            for(String permiso: listaPermisos) {
                if (checkSelfPermission(permiso) != PackageManager.PERMISSION_GRANTED) {
                    list.add(permiso);
                    habilitado = false;
                }else
                    habilitado=true;

                if(permiso.equals(Manifest.permission.CAMERA))
                    btnCamara.setEnabled(habilitado);
                else if (permiso.equals(Manifest.permission.MANAGE_EXTERNAL_STORAGE)  ||
                        permiso.equals(Manifest.permission.READ_EXTERNAL_STORAGE));

            }


        return list;
    }
    public void OCRfx(View v) {
    InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    recognizer.process(image)
            .addOnSuccessListener(this)
            .addOnFailureListener(this);
}


    @Override
    public void onFailure(@NonNull Exception e) {
        txtResults.setText("Error al procesar imagen");
    }

    @Override
    public void onSuccess(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
    String resultados="";
    if (blocks.size() == 0) {
        resultados = "No hay Texto";
    }else{
        for (int i = 0; i < blocks.size(); i++) {
           List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                 List<Text.Element> elements = lines.get(j).getElements();
                 for (int k = 0; k < elements.size(); k++) {
                     resultados = resultados + elements.get(k).getText() + " ";
                 }
            }
        }
        resultados=resultados + "\n";
    }
    txtResults.setText(resultados);
    }


    public void Rostrosfx(View  v) {
    InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();

    FaceDetector detector = FaceDetection.getClient(options);
    detector.process(image)
            .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                @Override
                public void onSuccess(List<Face> faces) {
                    if (faces.size() == 0) {
                        txtResults.setText("No Hay rostros");
                    }else{
                        txtResults.setText("Hay " + faces.size() + " rostro(s)");
                    }
                }
            })
            .addOnFailureListener(this);
}

    public void PersonalizedModel(View v) {
    try {
        Modelomascota model = Modelomascota.newInstance(getApplicationContext());
        TensorImage image = TensorImage.fromBitmap(mSelectedImage);

        Modelomascota.Outputs outputs = model.process(image);

        List<Category> probability = outputs.getProbabilityAsCategoryList();
        Collections.sort(probability, new CategoryComparator());

        String res="";
        for (int i = 0; i < probability.size(); i++) {
            res = res + probability.get(i).getLabel() +  " " +  probability.get(i).getScore()*100 + " % \n";
        }

        txtResults.setText(res);
        model.close();
    } catch (IOException e) {
        txtResults.setText("Error al procesar Modelo");
    }
}
    int previewHeight = 0,previewWidth = 0;
int sensorOrientation;
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
protected void setFragment() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    String cameraId = null;
    try {
        cameraId = manager.getCameraIdList()[0];
    } catch (CameraAccessException e) {
        e.printStackTrace();
    }
    CameraConnectionFragment fragment;
    CameraConnectionFragment camera2Fragment =
            CameraConnectionFragment.newInstance(
                    new CameraConnectionFragment.ConnectionCallback() {
                        @Override
                        public void onPreviewSizeChosen(final Size size, final int rotation) {
                            previewHeight = size.getHeight();    previewWidth = size.getWidth();
                            sensorOrientation = rotation - getScreenOrientation();
                        }
                    },
                    this,   R.layout.camerafragment,            new Size(640, 480));

    camera2Fragment.setCamera(cameraId);
    fragment = camera2Fragment;
    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
}
    protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_270:
            return 270;
        case Surface.ROTATION_180:
            return 180;
        case Surface.ROTATION_90:
            return 90;
        default:
            return 0;
    }
}
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private Bitmap rgbFrameBitmap;
    @Override
    public void onImageAvailable(ImageReader imageReader) {
        if (previewWidth == 0 || previewHeight == 0)           return;
    if (rgbBytes == null)    rgbBytes = new int[previewWidth * previewHeight];
    try {
        final Image image = imageReader.acquireLatestImage();
        if (image == null)    return;
        if (isProcessingFrame) {           image.close();            return;         }
        isProcessingFrame = true;
        final Image.Plane[] planes = image.getPlanes();
        fillBytes(planes, yuvBytes);
        yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();
        imageConverter =  new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420ToARGB8888( yuvBytes[0], yuvBytes[1], yuvBytes[2], previewWidth,  previewHeight,
                yRowStride,uvRowStride, uvPixelStride,rgbBytes);
                    }
                };
        postInferenceCallback =      new Runnable() {
                    @Override
                    public void run() {  image.close(); isProcessingFrame = false;  }
                };

        processImage();

    } catch (final Exception e) {    }
        
    }
    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
	for (int i = 0; i < planes.length; ++i) {
      		  final ByteBuffer buffer = planes[i].getBuffer();
       	  if (yuvBytes[i] == null) {
          		  yuvBytes[i] = new byte[buffer.capacity()];
        	}
        	buffer.get(yuvBytes[i]);
    }
}
    private void processImage() {
        //aqui colocar el modelo
    imageConverter.run();

    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
    rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
    try{
        classifyImage(rgbFrameBitmap);
    }
    catch (Exception e){
        txtResults.setText("Error al enviar la imagen");
        Log.i("error imagen", "processImage: "+e.getMessage());
    }
    postInferenceCallback.run();


    /*
    try {
        Modelomascota model = Modelomascota.newInstance(getApplicationContext());
        TensorImage image = TensorImage.fromBitmap(rgbFrameBitmap);

        Modelomascota.Outputs outputs = model.process(image);
        List<Category> probability = outputs.getProbabilityAsCategoryList();
        Collections.sort(probability, new CategoryComparator());
        String res="";
        for (int i = 0; i < probability.size(); i++) 
            res = res + probability.get(i).getLabel() +  " " +  probability.get(i).getScore()*100 + " % \n";
       
        txtResults.setText(res);
        model.close();
    } catch (IOException e) {
        txtResults.setText("Error al procesar Modelo");
    }
    */

}
    //modelo
    int imageSize = 224;
    TextToSpeech tts;

    public void classifyImage(Bitmap image){
    try {
       // txtResults.setText("sin al procesar el modelo");
        Model model = Model.newInstance(getApplicationContext());
        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, true);

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        // get 1D array of 224 * 224 pixels in image
        int [] intValues = new int[imageSize * imageSize];
        image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
        int pixel = 0;
        for(int i = 0; i < image.getHeight(); i++){
            for(int j = 0; j <  image.getWidth(); j++){
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }

        inputFeature0.loadBuffer(byteBuffer);

        // Runs model inference and gets result.
        Model.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        float[] confidences = outputFeature0.getFloatArray();
        // find the index of the class with the biggest confidence.
        int maxPos = 0;
        float maxConfidence = 0;
        for(int i = 0; i < confidences.length; i++){
            if(confidences[i] > maxConfidence){
                maxConfidence = confidences[i];
                maxPos = i;
            }
        }
        final String[] classes = {"Auditorio", "Biblioteca", "Centro Medico", "Comedor 2","Departamento Academico",
                "Departamento de Archivos","Departamento de Investigacion","Facultad Ciencias de la Salud","Facultad Ciencias Empresariales","Facultad de Ciencias Pedagogicas","Instituto de informatica",
                "Parqueadero  Estudiantes","Parqueadero Administrativo","Parqueadero autoridades","Polideportivo","Rectorado","Rotonda"};
        //txtResults.setText("aqui deberia salir la clase");
        String resultado = classes[maxPos];
        //txtResults.setText(classes[maxPos]);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtResults.setText(resultado);
            }
        });
        //invocar el text to speech
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status==TextToSpeech.SUCCESS)
                {
                    tts.setLanguage(new Locale("es", "ES"));
                    tts.setSpeechRate(1.0f);
                    tts.speak(resultado, TextToSpeech.QUEUE_ADD, null);
                }
            }
        });
        // Releases model resources if no longer used.
        model.close();
    } catch (Exception e) {
        //txtResults.setText("error");
        Log.i("error setzo", "classifyImage: "+e.getMessage());
    }
}
}


class CategoryComparator implements java.util.Comparator<Category> {
    @Override
    public int compare(Category a, Category b) {
        return (int)(b.getScore()*100) - (int)(a.getScore()*100);
    }
}

