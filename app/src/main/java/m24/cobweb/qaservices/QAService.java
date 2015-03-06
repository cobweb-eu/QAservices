package m24.cobweb.qaservices;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import org.geotools.feature.FeatureCollection;

import wps.client.service.GenericWPSClient;

/**
 * Created by lgzsam on 02/03/15.
 */
public class QAService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful

        String wpsURL = "http://geoprocessing.nottingham.ac.uk:8010/wps/WebProcessingService?";
        String processDescription = "pillar.cleaning.FilterOnAttribute";
        String catalogURL = null;


        HashMap<String, Object> wpsInputs = new HashMap<String, Object>();

        String inputObservations = "https://dyfi.cobwebproject.eu/geoserver/cobweb/" +
                "ows?service=WFS&version=1.0.0&request=GetFeature&typeName=" +
                "cobweb:observations&maxFeatures=50&outputFormat=text/xml;%20subtype=gml/3.1.1";

        String fieldName = "pos_tech";
        String featureName = "NETWORK";
        String include = "true";

        wpsInputs.put("fieldName", fieldName);
        wpsInputs.put("featureName", featureName);
        wpsInputs.put("include", include);


        GenericWPSClient wpsClient = new GenericWPSClient(wpsURL, processDescription, wpsInputs, null);
        FeatureCollection output = (FeatureCollection) wpsClient.getOutputs().get("result");
        Log.e("TAG", "wps output size " + output.size());




        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }
}
