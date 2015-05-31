/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import opennlp.maxent.BasicEventStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.AbstractModel;
import opennlp.model.EventStream;

/**
 *
 * @author Chris
 */
public class Final {

    /**
     * @param args the command line arguments
     */
    public class CountryStats{
        public int intTotal;
        public int intCorrect;
        Map<String,Integer>  all_countries;

        public CountryStats() {
            this.intTotal = 0;
            this.intCorrect = 0;
            this.all_countries = new TreeMap<String,Integer>();
        }

        public void addCountry(String strCountry) {
            int num_c = 1;
            if (all_countries.containsKey(strCountry))
            {
                num_c =  all_countries.get(strCountry);
                num_c = num_c + 1;
                all_countries.put(strCountry, num_c);
            }
            else
                all_countries.put(strCountry, num_c);
        }
        
        public double GetAccuracy(){
            return ((double)intCorrect/(double)intTotal) * 100.0;
        }
        
        public void PrintStats()
        {
            double frac = 0;
            System.out.print("\t");
            for (Map.Entry<String, Integer> entry : all_countries.entrySet()) {
                frac = (double)entry.getValue()/(double)intTotal;
                
                System.out.format("%s %.4f ", entry.getKey(), frac);
                
            }
            System.out.println();
        }
    }
    
    public static void TestF(String testFile, String trainFile)
    {        
                
        String dataFileName = "features";
        String modelFileName = "maxentfile";
        
        if (!trainFile.isEmpty())
        {
            String strInFile = trainFile;
            
            GenFeatures mkFeatures = new GenFeatures();
            
            mkFeatures.GenFeaturesFromFile(strInFile);

            mkFeatures.GenFeatureStrings(true);

            mkFeatures.WriteFeaturesToFile(dataFileName);

            try {
                FileReader datafr = new FileReader(new File(dataFileName));
                EventStream es = new BasicEventStream(new PlainTextByLineDataStream(datafr));
                GISModel model = GIS.trainModel(es, 100, 1);
                File outputFile = new File(modelFileName);
                GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, outputFile);
                writer.persist();
            } catch (Exception e) {
                System.out.print("Unable to create model due to exception: ");
                System.out.println(e);
            } 
        }
        
        
        if (!testFile.isEmpty())
        {
            String strInFile = testFile;
            
            GenFeatures mkFeatures = new GenFeatures();
            mkFeatures.GenFeaturesFromFile(strInFile);

            mkFeatures.GenFeatureStrings(false);


            Final.CountryStats cs = null;
            Final fc = new Final();
        
            Map<String,Final.CountryStats>  countries = new HashMap<String,Final.CountryStats>();
            String currentCountry = "";

            int nNumberOfCorrectTags = 0;

            try{
                AbstractModel m = new SuffixSensitiveGISModelReader(new File(modelFileName)).getModel();


                int nSize = mkFeatures.vctFeatureStrs.size();
                String strOutNameTag;
                String[] filename;
                if (strInFile.contains("."))                  
                    filename = strInFile.split("\\.");
                else
                {
                    filename = new String[2];
                    filename[0] = strInFile;
                    filename[1] = "";
                }

                BufferedWriter out = new BufferedWriter(new FileWriter(filename[0] + "_mytags." + filename[1]));
                for (int i = 0; i < nSize; i++)
                {
                    String[] contexts = mkFeatures.vctFeatureStrs.get(i).split(" ");
                    strOutNameTag = m.getBestOutcome(m.eval(contexts));

                    out.write(mkFeatures.vct.get(i).strSurname + " " + strOutNameTag);

                    out.newLine();                       

                    if (!mkFeatures.vct.get(i).strTag.equals(currentCountry))
                    {
                        if (countries.containsKey(mkFeatures.vct.get(i).strTag))
                            cs = countries.get(mkFeatures.vct.get(i).strTag);
                        else
                        {
                            cs = fc.new CountryStats();
                            countries.put(mkFeatures.vct.get(i).strTag, cs);
                        }
                        currentCountry = mkFeatures.vct.get(i).strTag;
                    }
                    
                    cs.intTotal++;
                    
                    cs.addCountry(strOutNameTag);
                    
                    // compute statistics
                    if (mkFeatures.vct.get(i).strTag.equals(strOutNameTag))
                    {
                        nNumberOfCorrectTags++;  
                        cs.intCorrect++;
                        
                    }
               }
                out.close();

                System.out.print("The new tagged file is: " + filename[0] + "_mytags." + filename[1] + "\n");

                double dTagAccuracy = (double) nNumberOfCorrectTags/nSize;
                dTagAccuracy *=100.0;
                System.out.format("%-40s%d%n", "Test sample size=", nSize);
                System.out.format("%-40s%.2f%%%n", "Name Tag Accuracy=", dTagAccuracy);
                
                System.out.format("%-40s%d%n", "Total number of countries=", countries.size());
                
                System.out.format("%-40s%n", "Accuracy per Country");
                for(String country: countries.keySet())
                {
                    System.out.format("%s%-20s%s%.2f%%%s%d%n", "Country: ", country, " Accuracy= ", countries.get(country).GetAccuracy(), " Training= ", countries.get(country).intTotal);
                    countries.get(country).PrintStats();
                }
                
                
                }catch(Exception e){
                    System.out.print("Unable to open model due to exception: ");
                    System.out.println(e);
                }    
        }       
    }
    
    public static void TestName(String name)
    {
        String modelFileName = "maxentfile";
        GenFeatures mkFeatures = new GenFeatures();
        
        mkFeatures.GenFeaturesFromName(name);
        mkFeatures.GenFeatureStrings(false);
        
        try{
            AbstractModel m = new SuffixSensitiveGISModelReader(new File(modelFileName)).getModel();

            String[] contexts = mkFeatures.vctFeatureStrs.get(0).split(" ");
            String allOut = m.getBestOutcome(m.eval(contexts));
            
            
            System.out.println(allOut);
            allOut = m.getAllOutcomes(m.eval(contexts));

            System.out.print(allOut);
            
                
            }catch(Exception e){
                System.out.print("Unable to open model due to exception: ");
                System.out.println(e);
            }        
    }
    
    public static void main(String[] args) 
    {         
        String trainFileName = "";
        String testFileName = "";
        String strName = "";
        
        
        for (int i =0; i < args.length; i++)
        {
            if (args[i].contains("-test") && args.length > i)
            {
                testFileName = args[i+1];
                i++;
            }
            else if (args[i].contains("-train") && args.length > i)
            {
                trainFileName = args[i+1];
                i++;
            }
            else if (args[i].contains("-name") && args.length > i)
            {
                strName = args[i+1];
                i++;
            }
                                
        }
                
        if (!testFileName.isEmpty() || !trainFileName.isEmpty())
            TestF(testFileName, trainFileName);

        if (!strName.isEmpty())
            TestName(strName);        
     }
}
