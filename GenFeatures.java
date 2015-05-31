/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.text.Normalizer;

/**
 *
 * @author Chris
 */
public class GenFeatures {   
     public class NameData{
        public String strTag;
        public String strSurname;
        public String [] strNames;

        public String getStrNames() {
            String strTotal = "";
            for (int i=0; i<strNames.length; i++)
                strTotal = strTotal + strNames[i] + " ";
            
            return strTotal.trim();
        }

        public NameData() {
            strTag = "";
            strSurname = "";
            strNames = new String[0];
        }
        
    }
    public class Strip {
        public String flattenToAscii(String string) 
        {
            StringBuilder sb = new StringBuilder(string.length());
            string = Normalizer.normalize(string, Normalizer.Form.NFD);
            for (char c : string.toCharArray()) 
            {
                if (c <= '\u007F') sb.append(c);
            }
            return sb.toString();
        }
        
        public String flattenToAscii2(String string) 
        {            
            return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        }
    }    
    int MAXSIZE = 10000;
    /*
    String m_strAllVowel = "AEIOU";
    String m_strA = "A";
    String m_strE = "E";
    String m_strI = "I";
    String m_strO = "O";
    String m_strU = "U";
    */
    String m_strAllVowel = "aeiou";
    String m_strA = "a";
    String m_strE = "e";
    String m_strI = "i";
    String m_strO = "o";
    String m_strU = "u";
    
    public ArrayList<NameData> vct;
    public ArrayList<String> vctFeatureStrs;
    public String[] vctFeaturesToUse;
    public Strip strStrip;

    public GenFeatures() {
        strStrip = new Strip();
        
        // list of all available features
        // strFeatures = "unigramBEG unigramEND bigramBEG bigramMID bigramEND trigramBEG trigramMID trigramEND VowelsRatio NumConsonants NumVowels Length MeanDistVowels
        //              numA numE numI numO numU bigramRVBEG bigramRVEND bigramRVMID trigramRVBEG trigramRVEND trigramRVMID
        //              bigramRBEG bigramREND bigramRMID trigramRBEG trigramREND trigramRMID";
        // AEIOU
        // strFeatures = "unigramBEG unigramEND bigramBEG bigramMID bigramEND trigramBEG trigramMID trigramEND NumConsonants NumVowels MeanDistVowels Length";
        // strFeatures = "unigramEND bigramBEG bigramEND trigramBEG trigramMID trigramEND VowelsRatio MeanDistVowels Length"; 60-68%
        //String strFeatures = "bigramBEG bigramEND";
        //String strFeatures = "bigramBEG bigramEND trigramBEG trigramMID trigramEND VowelsRatio MeanDistVowels Length bigramRVBEG bigramRVEND bigramRVMID trigramRVBEG trigramRVEND trigramRVMID";
        
        
        String strFeatures = "unigramEND bigramBEG bigramEND trigramBEG trigramMID trigramEND VowelsRatio MeanDistVowels Length bigramRBEG bigramREND bigramRMID trigramRBEG trigramREND trigramRMID";
        
        
        vctFeaturesToUse = strFeatures.split("\\s+");                
        
        vct = null;
    }

    public void GenFeaturesFromName(String name)
    {
        
        if (vct == null)
            vct = new ArrayList<NameData>(1);
        NameData ftr = new NameData();        
        
        ftr.strSurname = name.toLowerCase().trim();
        vct.add(0, ftr);
    }
    public void GenFeaturesFromFile(String filename)
    {            
        try{

            File file = new File(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

            br.mark(1);
            if (br.read() != 0xFEFF)
                br.reset();
            
            vct = new ArrayList<NameData>(MAXSIZE);

            String line = null;
            while( (line = br.readLine())!= null )
            {
                if (!line.isEmpty())
                {
                    NameData ftr = new NameData();
                    // \\s+ means any number of whitespaces between tokens
                    String [] tokens = line.split("\\s+");
                    
                    
                    ftr.strNames = new String[tokens.length-2];
                    
                    for (int i=0; i < tokens.length-2; i++)
                        // for now lets ignore spaces this could change
                        ftr.strNames[i] = tokens[i];
 
                    ftr.strSurname = tokens[tokens.length-2];
                    ftr.strTag = tokens[tokens.length-1];
                    
                    
                    vct.add(ftr);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
     
    public void GenFeatureStrings(boolean bTag)
    {
        int nSize = vct.size();
        
        Class[] cArg = new Class[1];
        cArg[0] = String.class;
        
        Method[] m = new Method[vctFeaturesToUse.length];
        try
        {
            for (int fdef=0;fdef<vctFeaturesToUse.length;fdef++)
            {
                m[fdef] = (this.getClass().getMethod(vctFeaturesToUse[fdef], cArg));
            } 
        
            vctFeatureStrs = new ArrayList<String>(MAXSIZE);
            for (int i = 0; i < nSize; i++)
            {
                String newFeature = "";
                   
                // add name1, name2...
                for (int name=0; name < vct.get(i).strNames.length; name++)
                    newFeature = newFeature + "name" + Integer.toString(name) + "=" + vct.get(i).strNames[name] +  " ";
                
                for (int fdef=0;fdef<vctFeaturesToUse.length;fdef++)
                {
                    //vct.get(i).strSurname
                   newFeature = newFeature + vctFeaturesToUse[fdef] + "=" + m[fdef].invoke(this, vct.get(i).strSurname) + " ";
                }

                if (bTag)
                    newFeature = newFeature + vct.get(i).strTag;
                else
                    newFeature = newFeature.trim();

                vctFeatureStrs.add(newFeature);
            }
        
        }catch(Exception e){
            System.out.println(e);            
        }        
    }    

    public void WriteFeaturesToFile(String filename)
    {
        try{
            int nSize = vctFeatureStrs.size();
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            for (int i = 0; i < nSize; i++)
            {
                out.write(vctFeatureStrs.get(i));
                    
                out.newLine();
            }
            
            out.close();
            
            System.out.print("features file created\n");
        }catch(Exception e){
            e.printStackTrace();
        }       
    }
    
    private int intNumVowels(String name)
    {
        String strVowels = m_strAllVowel;
        name = strStrip.flattenToAscii2(name);
        
        int nVowels = 0;
        for (int i=0; i < name.length(); i++)
        {
            String myChar = Character.toString(name.charAt(i));
            if (strVowels.contains(myChar))
                nVowels++;
        }
       
        return nVowels;
    }
    // feature functions
    
    // bigrams
    public String bigramBEG(String name)
    {
        if (name.length() > 2)
            return name.substring(0, 2);
                
        return name;
    }
    
    public String bigramEND(String name)
    {
        if (name.length() > 2)
            return name.substring(name.length()-2, name.length());
                
        return name;
    }    
    
    public String bigramMID(String name)
    {
        if (name.length() > 2)
        {
            double dVal = (double)name.length()/2.0;
                    
            int idx = (int)Math.ceil(dVal);
            
            return name.substring(idx-1, idx+1);
        }
        
        return name;
    }
    
    //unigrams
    public String unigramBEG(String name)
    {
        if (name.length() > 1)
            return name.substring(0, 1);
                
        return name;
    }
    
    public String unigramEND(String name)
    {
        if (name.length() > 1)
            return name.substring(name.length()-1, name.length());
                
        return name;
    }    
    
    // trigrams
    public String trigramBEG(String name)
    {
        if (name.length() > 3)
            return name.substring(0, 3);
                
        return name;
    }
    
    public String trigramEND(String name)
    {
        if (name.length() > 3)
            return name.substring(name.length()-3, name.length());
                
        return name;
    }    
    
    public String trigramMID(String name)
    {
        if (name.length() > 3)
        {
            double dVal = (double)name.length()/2.0;
                    
            int idx = (int)Math.floor(dVal);
            
            return name.substring(idx-1, idx+2);
        }
        
        return name;
    }
    // others
    
    // ratio of  Consonants to Vowels
    
    public String VowelsRatio(String name)
    {         
        int nVowels = intNumVowels(name);
        
        double dRatio = (double)nVowels/(double)name.length();
        
        int nRatio = (int)(dRatio*100.0);
        
        return Integer.toString(nRatio);
    }

    public String Length(String name)
    {
        return Integer.toString(name.length());
    }
    
    public String NumVowels(String name)
    {
        int nVowels = intNumVowels(name);
        return Integer.toString(nVowels);
    }
           
    public String NumConsonants(String name)
    {
        int nVowels = intNumVowels(name);
        int nCon = name.length() - nVowels;
        return Integer.toString(nCon);
    }
    
    // mean distance between vowels
    public String MeanDistVowels(String name)
    {
        int totalDist = 0;
        int numDist = -1;
        int nLastV = 0;
        
        String strVowels = m_strAllVowel;
        name = strStrip.flattenToAscii2(name);
        
        for (int i=0; i < name.length(); i++)
        {
            String myChar = Character.toString(name.charAt(i));
            if (strVowels.contains(myChar))                
            {
                numDist++;
                if (numDist == 0)
                    nLastV = i;
                
                totalDist = totalDist + (i-nLastV);
                
                nLastV = i;
                
            }
        }        
        
        double dVal = -1.0;
        if(numDist != 0)
            dVal = (double)totalDist/(double)numDist;
        
        
        return Integer.toString((int)(dVal*10.0));
    }
    
    private int numVowel(String name, String strChar)
    {
        int nCount = 0;
        name = strStrip.flattenToAscii2(name);
        for (int i=0; i < name.length(); i++)
        {
            String myChar = Character.toString(name.charAt(i));
            if (strChar.contains(myChar))                
                nCount++;
        }           
        
        return nCount;
    }
    
    public String numA(String name)
    {
        return Integer.toString(numVowel(name, m_strA));
    }
    public String numE(String name)
    {
        return Integer.toString(numVowel(name, m_strE));
    }   
    public String numI(String name)
    {
        return Integer.toString(numVowel(name, m_strI));
    }
    public String numO(String name)
    {
        return Integer.toString(numVowel(name, m_strO));
    }
    public String numU(String name)
    {
        return Integer.toString(numVowel(name, m_strU));
    }
    
    // more n-grams
    
    private String strReduceToVowels(String name)
    {
        String strVowels = m_strAllVowel;
        name = strStrip.flattenToAscii2(name);
        
        String ret = "";
        
        for (int i=0; i < name.length(); i++)
        {
            String myChar = Character.toString(name.charAt(i));
            if (!strVowels.contains(myChar))
                ret = ret + "C";
            else
                ret = ret + myChar;
        }
       
        return ret;
    }    
    // bigram reduced to Vowels
    public String bigramRVBEG(String name)
    {
        if (name.length() > 2)
            name = name.substring(0, 2);
                
        return strReduceToVowels(name);
    }
    
    public String bigramRVEND(String name)
    {
        if (name.length() > 2)
            name =  name.substring(name.length()-2, name.length());
                
        return strReduceToVowels(name);
    }    
    
    public String bigramRVMID(String name)
    {
        if (name.length() > 2)
        {
            double dVal = (double)name.length()/2.0;
                    
            int idx = (int)Math.ceil(dVal);
            
            name =  name.substring(idx-1, idx+1);
        }
        
        return strReduceToVowels(name);
    }  
    
    // trigrams reduced to Vowels
    public String trigramRVBEG(String name)
    {
        if (name.length() > 3)
            name =  name.substring(0, 3);
                
        return strReduceToVowels(name);
    }
    
    public String trigramRVEND(String name)
    {
        if (name.length() > 3)
            name =  name.substring(name.length()-3, name.length());
                
        return strReduceToVowels(name);
    }    
    
    public String trigramRVMID(String name)
    {
        if (name.length() > 3)
        {
            double dVal = (double)name.length()/2.0;
                    
            int idx = (int)Math.floor(dVal);
            
            name =  name.substring(idx-1, idx+2);
        }
        
        return strReduceToVowels(name);
    }  
    
// more n-grams
    
    private String strReduce(String name)
    {
        String strVowels = m_strAllVowel;
        name = strStrip.flattenToAscii2(name);
        
        String ret = "";
        
        for (int i=0; i < name.length(); i++)
        {
            String myChar = Character.toString(name.charAt(i));
            if (!strVowels.contains(myChar))
                ret = ret + "C";
            else
                ret = ret + "V";
        }
       
        return ret;
    }    
    // bigram reduced
    public String bigramRBEG(String name)
    {
        if (name.length() > 2)
            name = name.substring(0, 2);
                
        return strReduce(name);
    }
    
    public String bigramREND(String name)
    {
        if (name.length() > 2)
            name =  name.substring(name.length()-2, name.length());
                
        return strReduce(name);
    }    
    
    public String bigramRMID(String name)
    {
        if (name.length() > 2)
        {
            double dVal = (double)name.length()/2.0;
                    
            int idx = (int)Math.ceil(dVal);
            
            name =  name.substring(idx-1, idx+1);
        }
        
        return strReduce(name);
    }  
    
    // trigrams reduced to Vowels
    public String trigramRBEG(String name)
    {
        if (name.length() > 3)
            name =  name.substring(0, 3);
                
        return strReduce(name);
    }
    
    public String trigramREND(String name)
    {
        if (name.length() > 3)
            name =  name.substring(name.length()-3, name.length());
                
        return strReduce(name);
    }    
    
    public String trigramRMID(String name)
    {
        if (name.length() > 3)
        {
            double dVal = (double)name.length()/2.0;
                    
            int idx = (int)Math.floor(dVal);
            
            name =  name.substring(idx-1, idx+2);
        }
        
        return strReduce(name);
    }        
}
