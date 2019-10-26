package com.ibmhackaton.watchasaid;
/*
Authors: Michy en Jeroen
 */

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.function.Consumer;

class BaseRecognizeCallbackResult extends BaseRecognizeCallback {

    private MainActivity callback;

    BaseRecognizeCallbackResult(MainActivity activity) {
        this.callback = activity;
    }

    @Override
    public void onTranscription
            (SpeechRecognitionResults speechRecognitionResults) {
        String result = speechRecognitionResults.toString();
        callback.saveRecordingText(result);
    }

}

public class RecordingProcessor {

    public void process(String file_loc, MainActivity activity) {

        String api_key = "a5kj54yN6Q9bacmTD4XwlHITLWU8gtR5zPXtag951JCg";
        String url = "https://stream-fra.watsonplatform.net/speech-to-text/api";
        //final String[] transcription = new String[1];

        IamAuthenticator authenticator = new IamAuthenticator(api_key);
        SpeechToText speechToText = new SpeechToText(authenticator);
        speechToText.setServiceUrl(url);

        BaseRecognizeCallbackResult baseRecognizeCallback =
                new BaseRecognizeCallbackResult(activity);

        try {
            RecognizeOptions recognizeOptions = new RecognizeOptions.Builder()
                    .audio(new FileInputStream(file_loc))
                    .contentType("audio/wav")
                    .model("en-US_BroadbandModel")
                    //.keywords(Arrays.asList("colorado", "tornado", "tornadoes"))
                    //.keywordsThreshold((float) 0.5)
                    //.maxAlternatives(1)
                    .build();

            speechToText.recognizeUsingWebSocket(recognizeOptions,
                    baseRecognizeCallback);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


}




/*
"apikey": "a5kj54yN6Q9bacmTD4XwlHITLWU8gtR5zPXtag951JCg",
  "iam_apikey_description": "Auto-generated for key 396c97fb-db0f-4d46-888a-e8d484be9aaa",
  "iam_apikey_name": "Auto-generated service credentials",
  "iam_role_crn": "crn:v1:bluemix:public:iam::::serviceRole:Manager",
  "iam_serviceid_crn": "crn:v1:bluemix:public:iam-identity::a/1ae905fe80104aad92e1c579ad34555b::serviceid:ServiceId-db5a227b-2d4a-4219-9368-50a1f26161e4",
  "url": "https://stream-fra.watsonplatform.net/speech-to-text/api"

curl -X POST -u "apikey:a5kj54yN6Q9bacmTD4XwlHITLWU8gtR5zPXtag951JCg" \
--header "Content-Type: audio/flac" \
--data-binary @/mnt/c/Users/michi/Downloads/testNoise.flac \
"https://stream-fra.watsonplatform.net/speech-to-text/api/v1/recognize"
*/