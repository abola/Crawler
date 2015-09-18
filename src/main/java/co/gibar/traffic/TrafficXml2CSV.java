package co.gibar.traffic;

import co.gibar.crawler.StoreTools;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.File;

/**
 * Created by abolalee on 2015/9/18.
 */
public class TrafficXml2CSV {

    public TrafficXml2CSV transfer(String xml){



        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());

        String day = doc.select("XML_Head").attr("updatetime").toString().replaceAll("/","").substring(0,8);

//        System.out.println("/traffic/" + day + ".csv");
        StringBuilder sb = new StringBuilder();


        for (Element e : doc.select("Info")) {
            sb.append(e.attr("vdid"));
            sb.append("," + e.attr("datacollecttime"));
            sb.append("," + e.select("lane").attr("vsrid"));
            sb.append("," + e.select("lane").attr("speed"));
            sb.append("," + e.select("lane").attr("laneoccupy"));
            sb.append("," + e.select("lane cars[carid=S]").attr("volume"));
            sb.append("," + e.select("lane cars[carid=T]").attr("volume"));
            sb.append("," + e.select("lane cars[carid=L]").attr("volume"));
            //System.out.print(","+e.select("lane cars[carid=S]"));
            sb.append("\n");
        }


        StoreTools.startStdoutTo("/traffic/" + day + ".csv");
        System.out.print(sb.toString());
        StoreTools.resetStdout();

        return this;

    }

    public static TrafficXml2CSV create(){
        return new TrafficXml2CSV();
    }

    public static void main(String[] argv){


            for( String path : argv[0].split(",") ){

                if (! "".equals(path.trim()) ){

                    try{
                        StringBuilder xml = new StringBuilder();
                        File file = new File(argv[0]);


                        for (String line : Files.readLines(file, Charsets.UTF_8)) {
                            xml.append(line.trim() + "\n");
                        }

                        TrafficXml2CSV
                                .create()
                                .transfer(xml.toString())
                        ;
                        System.err.println("[o]file:" + argv[0]);
                    }catch (Exception ex){
                        System.err.println("[x]file:" + argv[0]);
                    }
                }

            }

    }
}
