package Features;

import de.bwaldvogel.liblinear.*;

import java.util.*;
import java.io.*;

public class CreateFeatures {
    public Map<String,Integer> labels_map;
    public Map<String,Integer> feature_map;
    public Map<Integer,String> reverse_map;

    public ArrayList<ArrayList<String>> trainfilecontent;
    public ArrayList<ArrayList<String>> testfilecontent;

    private Problem problem = null;
    private Model model;
    private File modelFile;

    public CreateFeatures(){
        //System.out.println("Constructor");
        labels_map = new HashMap<String,Integer>();
        feature_map = new HashMap<String,Integer>();
        reverse_map = new HashMap<>();
        trainfilecontent = new ArrayList<>();
        testfilecontent = new ArrayList<>();
    }

    public void createFeatureMap(){
        int k = 0;
        int j = 1;
        for(ArrayList<String> strlist : trainfilecontent){
            if(strlist.size() != 0) {
                for(int i = 0;i<strlist.size();i++){
                    String val = strlist.get(i);
                    if(i == 0){
                        int ndx = val.indexOf('+');
                        if(ndx  == -1) {
                            if(!labels_map.containsKey(val)) {
                                labels_map.put(val, k++);
                                reverse_map.put(labels_map.get(val),val);
                            }
                        }
                        else{
                            String v = val.substring(0,val.indexOf('+'));
                            if(!labels_map.containsKey(v)) {
                                labels_map.put(v, k++);
                                reverse_map.put(labels_map.get(val),val);
                            }
                        }
                    }
                    else{
                        if(!val.isEmpty() && !feature_map.containsKey(val)){
                            feature_map.put(strlist.get(i),j++);
                        }
                    }
                }
            }
        }
        feature_map.put("UNDEFINED",j);
        //feature_map.put("PHI",j+1);
        //feature_map.put("OMEGA",j+2);

        System.out.println("No. of Features: "+feature_map.size());
        System.out.println("No. of Labels: "+labels_map.size());
        System.out.println(labels_map);
    }

    public String createFeatures(ArrayList<String> temp){
        HashSet<Integer> s = new HashSet<>();
        StringBuffer sb = new StringBuffer();
        for (int j = 0; j < temp.size(); j++) {
            if (j == 0) {
                String val = temp.get(j);
                if(val.indexOf('+') == -1)
                    sb.append(Integer.toString(labels_map.get(val)) + " ");
                else{
                    String v = val.substring(0,val.indexOf('+'));
                    sb.append(Integer.toString(labels_map.get(v)) + " ");
                }
            }
            else{
                if(!temp.get(j).isEmpty()) {
                    if (!feature_map.containsKey(temp.get(j))) {
                        s.add(feature_map.get("UNDEFINED"));
                    } else {
                        s.add(feature_map.get(temp.get(j)));
                    }
                }
            }
//            if (i == 0 || filecontent.get(i - 1).size() == 0){
//                s.add(feature_map.get("PHI"));
//            }
//            if(i == filecontent.size()-1 || filecontent.get(i + 1).size() == 0 ){
//                s.add(feature_map.get("OMEGA"));
//            }
        }
        List<Integer> sortedList = new ArrayList<Integer>(s);
        Collections.sort(sortedList);

        for(Integer a: sortedList){
            sb.append(a.toString()+":1 ");
        }
        sb.append("\n");
        return sb.toString();
    }
    public void writeFeatures(String outfilename,String type){
        ArrayList<ArrayList<String>> filecontent = null;

        if(type == "train")
            filecontent = trainfilecontent;
        else
            filecontent = testfilecontent;
        try {
            // FileReader reads text files in the default encoding.
            FileWriter fileWriter =
                    new FileWriter(outfilename);

            // Always wrap FileReader in BufferedReader.
            BufferedWriter bufferedWriter =
                    new BufferedWriter(fileWriter);

            for(int i = 0;i<filecontent.size();i++) {
                ArrayList<String> temp = filecontent.get(i);
                if (temp.size() != 0) {
                    bufferedWriter.write(createFeatures(temp));
                }
            }

            // Always close files.
            bufferedWriter.close();
            fileWriter.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            outfilename + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + outfilename + "'");
            // Or we could just do this:
            // ex.printStackTrace();
        }

    }


    public ArrayList<ArrayList<String>> readFileContent(String filename){
        String line;
        ArrayList<ArrayList<String>> temp = new ArrayList<>();
        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader =
                    new FileReader(filename);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader =
                    new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                if(!line.isEmpty())
                    temp.add(new ArrayList<String>(Arrays.asList(line.trim().split(" "))));
            }

            // Always close files.
            bufferedReader.close();
            fileReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            filename + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + filename + "'");
            // Or we could just do this:
            // ex.printStackTrace();
        }
        return temp;
    }

    public void run(String trainfile,String testfile,String trainoutfile,String testoutfile) {

        String trainfilename = trainfile;
        String testfilename = testfile;
        String trainoutfilename = trainoutfile;
        String testoutfilename = testoutfile;

        CreateFeatures m = new CreateFeatures();

        m.trainfilecontent = m.readFileContent(trainfilename);
        System.out.println("Training instances: "+m.trainfilecontent.size());
        m.testfilecontent = m.readFileContent(testfilename);
        System.out.println("Test instances: "+m.testfilecontent.size());
        m.createFeatureMap();

        m.writeFeatures(trainoutfilename,"train");
        m.writeFeatures(testoutfilename,"test");
    }

    public void Train(File trainfile){
        try {
            problem = Problem.readFromFile(trainfile, 0.0);
        }catch(InvalidInputDataException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        SolverType solver = SolverType.L2R_LR; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter parameter = new Parameter(solver, C, eps);
        try {
            model = Linear.train(problem, parameter);
            modelFile = new File("model");

            try {
                model.save(modelFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }catch(NullPointerException e){
            e.printStackTrace();
        }

    }

    public String predict(String line){
//        model = Model.load(modelFile);

        String features = createFeatures(new ArrayList<String>(Arrays.asList(line.trim().split(" "))));

        ArrayList<String> feats= new ArrayList<String>(Arrays.asList(features.trim().split(" ")));

        Feature[] instance = new Feature[feats.size()-1];

        for(int i=1;i<feats.size();i++){
            String temp = feats.get(i);
            int ndx = temp.indexOf(':');
            int a = Integer.parseInt(temp.substring(0,ndx));
            int b = Integer.parseInt(temp.substring(ndx+1));
            instance[i-1] = new FeatureNode(a,b);
        }

//        Feature[] instance = { new FeatureNode(1, 4), new FeatureNode(2, 2) };
        double prediction = Linear.predict(model, instance);

        return reverse_map.get(prediction);
    }

}