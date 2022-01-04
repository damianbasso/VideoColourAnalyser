/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package VideoColourAnalyser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.opencv.video.Video;

public class VideoScanner implements java.io.Serializable{
    
    // The video to be scanned
    // private File file;

    private String filmname;

    public String getFileName(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0 && pos < (fileName.length() - 1)) { // If '.' is not the first or last character.
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }


    public void setFilmname(String filename) {        
        this.filmname = filename;
        System.out.println(filmname);
    }

    public String getFilmname() {
        return filmname;
    }

    // All the colours used in the video, and the number of pixels of that colour
    private HashMap<Color, Integer> colors = new HashMap<>();
    private double fps = 0;
    // Lists the average colour of each frame of the video in order
    private List<Color> aveColorsInOrder = null;
    // // The calculated dominant colours in the image that can be found after processing
    // private ArrayList<ColorWeight> dominantColours = null;

    private static final long serialVersionUID = 1111111;

    public VideoScanner(String filename) {
        this.filmname = filename;
    }

    // public String getFileName() {
    //     String fileName = file.getName();
    //     int pos = fileName.lastIndexOf(".");
    //     if (pos > 0 && pos < (fileName.length() - 1)) { // If '.' is not the first or last character.
    //         fileName = fileName.substring(0, pos);
    //     }
    //     return fileName;
    // }

    public double getFps() {
        return fps;
    }

    

    
    private void processVideo(int targFPS) throws FFmpegFrameGrabber.Exception {
        // StopWatch();
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber("films/" + filmname + ".mp4");
        frameGrabber.setFormat("mp4");
        // frameGrabber.setFrameRate(2.0);
        // FFmpegLogCallback.set();
        frameGrabber.start(); 

        if (targFPS > frameGrabber.getFrameRate()) {
            fps = frameGrabber.getFrameRate();
        }
        else {
            fps = targFPS;
        }
        Frame f; 
        System.out.println("frame rate is " + frameGrabber.getVideoFrameRate() + " with " + frameGrabber.getLengthInVideoFrames());

        Java2DFrameConverter c = new Java2DFrameConverter(); 
        int totalFrames = 0;
        double includedFrames = 0;
        
        // The ratio of frames to be included to the base frame rate of the video.
        double FPSConversionRate = targFPS/frameGrabber.getFrameRate();
        aveColorsInOrder = new ArrayList<>();
        // StopWatch("Initialisation done");

        while ((f = frameGrabber.grab()) != null) {
            try {
                // StopWatch();
                BufferedImage bi = c.convert(f);;
                // StopWatch("Converted and checked");

                if (bi == null) {
                    continue;
                }
                totalFrames++;
                // StopWatch();
                aveColorsInOrder.add(ColorWeight.averageWeights(bi));
                // StopWatch("lemons");
                // StopWatch("average Color added");
                if (includedFrames/totalFrames < FPSConversionRate) {
                    // StopWatch();
                    // StopWatch();
                    addImageToColors(bi);
                    // StopWatch("orange");
                    // StopWatch("Colours added to total");
                    includedFrames++;
                    // System.out.println(includedFrames + "SSSSS");
                    if (includedFrames % Math.round(fps)* 3 == 0) {
                        System.out.println(totalFrames + "/" + frameGrabber.getLengthInVideoFrames());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        c.close();
        frameGrabber.stop();
    }

    private static List<ColorWeight> parseImage(BufferedImage image) throws IOException {
        HashMap<Color,Integer> colors = new HashMap<>();
        for (int x = 0; x <image.getWidth(); x++) {
            for (int y = 0; y< image.getHeight(); y++) {
                int clr = image.getRGB(x, y);
                Color color = new Color(clr);
                if (colors.containsKey(color)) {
                    colors.put(color, colors.get(color) + 1);
                }
                else {
                    colors.put(color, 1);    
                }
            }
        }
        List<ColorWeight> colorsByWeight = new ArrayList<>();
        for (Map.Entry<Color, Integer> entry : colors.entrySet()) {
            colorsByWeight.add(new ColorWeight(entry.getKey(), entry.getValue()));
        }
        colorsByWeight.sort(new ByHSB());
        return colorsByWeight;
    }

    private void addImageToColors(BufferedImage image) {
        for (int x = 0; x <image.getWidth(); x++) {
            for (int y = 0; y< image.getHeight(); y++) {
                int clr = image.getRGB(x, y);
                Color color = new Color(clr);
                if (colors.containsKey(color)) {
                    colors.put(color, colors.get(color) + 1);
                }
                else {
                    colors.put(color, 1);    
                }
            }
        }
    }

    private void formDissect() throws IOException {
        VideoDissect rect = new VideoDissect(aveColorsInOrder);
        rect.save("VideoData/" + filmname + "/Dissect.png");
        JFrame window = new JFrame();
        window.setLayout(new BorderLayout());
        window.getContentPane().add(rect);
        window.pack();
        // window.setSize(200, 200);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        window.setVisible(true);
    }

    private void graphDoms(int k) {
        DominantMapper dm = new DominantMapper(this.getColorWeights(), filmname);
        dm.graphColour(k);
    }



    public List<ColorWeight> getColorWeights() {
        List<ColorWeight> cws = new ArrayList<>();
        for (Color cw : colors.keySet()) {
            cws.add(new ColorWeight(cw, colors.get(cw)));
        }
        return cws;
    }

    private static VideoScanner deserialiseVideoData(String fileName) {
        VideoScanner vs = null;
        try {
            FileInputStream fileIn = new FileInputStream("VideoData/" + fileName + ".ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            vs = (VideoScanner) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vs;
    }

    private static VideoScanner deserialiseVideoData(String fileName, int fps) {
        VideoScanner vs = null;
        try {
            FileInputStream fileIn = new FileInputStream("VideoData/" + fileName + "/" + fileName + "_" + fps + "fps.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            vs = (VideoScanner) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vs;
    }

    private void findAndSaveDoms() throws IOException {
        DominantMapper dm = new DominantMapper(this);
        dm.findDominantColors2();
        dm.save();
    }   

    /**
     * Ensures that the film has a folder stored in VideoData
     */
    private void ensureDirExists() {
        Path path = Paths.get("VideoData/" + this.filmname);
        if (Files.notExists(path)) {
            File f = new File("VideoData");  
		    f.mkdir(); 
        }

    }

    // returns how many seconds it took
    private static long serialiseVideo(String filename, int frameRate) throws FFmpegFrameGrabber.Exception {
        VideoScanner app = new VideoScanner(filename);
        long startime = System.nanoTime();
        app.processVideo(frameRate);
        long timeTaken = (System.nanoTime() - startime)/1000000000;
        System.out.println("Video processed in " + timeTaken + " seconds");
        
        // Serialise
        try {
            app.ensureDirExists();
            FileOutputStream outputFile = new FileOutputStream("VideoData\\" + app.getFilmname() + "\\" + app.getFilmname() + "_" + Math.round(app.getFps()) + "fps.ser");
            ObjectOutputStream out = new ObjectOutputStream(outputFile);

            // Method for serialization of object
            out.writeObject(app);

            out.close();
            outputFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timeTaken;
    }

    public static void main(String []args) throws IOException, Exception
    {
        // serialiseVideo("Zootopia", 24);
        // serialiseVideo("All_of_the_Lights", 24);
        // DominantMapper dm = new DominantMapper(deserialiseVideoData("All_of_the_Lights",3));
    
        // for (File film : new File("VideoData").listFiles()) {
        //     System.out.println(film.getName());
        // }
        
        
        // System.out.println("Kung_Fu_Panda");
        // deserialiseVideoData("Kung_Fu_Panda",24).formDissect();

        
        System.out.println("The_Grinch");
        deserialiseVideoData("The_Grinch",24).findAndSaveDoms();

        
        System.out.println("The_Hunger_Games");
        deserialiseVideoData("The_Hunger_Games",24).findAndSaveDoms();

        System.out.println("The_Irishmen");
        deserialiseVideoData("The_Irishmen",24).findAndSaveDoms();

        System.out.println("Wreck_it_Ralph");
        deserialiseVideoData("Wreck_it_Ralph",24).findAndSaveDoms();

        //     System.out.println(film.getName());
        //     VideoScanner app = deserialiseVideoData(film.getName(),24);
        //     app.setFilmname(film.getName());
        //     FileOutputStream outputFile = new FileOutputStream(film + "\\" + app.getFilmname() + "_" + Math.round(app.getFps()) + "fps.ser");
        //     ObjectOutputStream out = new ObjectOutputStream(outputFile);

        //     // Method for serialization of object
        //     out.writeObject(app);

        //     out.close();
        //     outputFile.close();
        // }
        // File dir = new File("VideoData");
        // for(File file : dir.listFiles()) {
        //     File curr = new File("VideoData/" + file.getName().substring(0, file.getName().length()-10));
        //     curr.mkdir();
        // }
    }
}
