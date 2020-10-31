package org.deeplearning4j.examples.feedforward.mnist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Dictionary;
import java.util.Hashtable;

import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSTMNetwork {

    private static final Logger log = LoggerFactory.getLogger(LSTMNetwork.class);

    //'baseDir': Base directory for the data. Change this if you want to save the data somewhere else
    private static File baseDir = new File("src/main/resources/uci/");
    private static File baseTrainDir = new File(baseDir, "train");
    private static File featuresDirTrain = new File(baseTrainDir, "features");
    private static File labelsDirTrain = new File(baseTrainDir, "labels");
    private static File baseTestDir = new File(baseDir, "test");
    private static File featuresDirTest = new File(baseTestDir, "features");
    private static File labelsDirTest = new File(baseTestDir, "labels");

    public static void main(String[] args) throws Exception {

        int numLabelClasses = 0;
        Dictionary dict = new Hashtable();
        File dictFile = new File("src/main/resources/label_dict.txt");
        BufferedReader br = new BufferedReader(new FileReader(dictFile));
        String line, label;
        int labelCode;
        while ((line = br.readLine()) != null){
            System.out.println(line);
            if(line.split(":").length > 1) {
                label = line.split(":")[0];
                labelCode = Integer.parseInt(line.split(":")[1]);
                dict.put(labelCode, label);
                numLabelClasses += 1;
            }
        }

        // ----- Load the training data -----
        //Note that we have 450 training files for features: train/features/0.csv through train/features/449.csv
        int miniBatchSize = 10;
        int nSamples = (new File(featuresDirTrain.getAbsolutePath())).list().length/miniBatchSize * miniBatchSize;

        SequenceRecordReader trainFeatures = new CSVSequenceRecordReader();
        trainFeatures.initialize(new NumberedFileInputSplit(featuresDirTrain.getAbsolutePath() + "/X%d.csv", 0, nSamples));
        SequenceRecordReader trainLabels = new CSVSequenceRecordReader();
        trainLabels.initialize(new NumberedFileInputSplit(labelsDirTrain.getAbsolutePath() + "/X%d.csv", 0, nSamples));


        DataSetIterator trainData = new SequenceRecordReaderDataSetIterator(trainFeatures, trainLabels, miniBatchSize, numLabelClasses,
            false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);


        INDArray array = trainData.next(1).getFeatures();
        // ----- Configure the network -----
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(123)    //Random number generator seed for improved repeatability. Optional.
            .weightInit(WeightInit.XAVIER)
            .updater(new Nadam())
            .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)  //Not always required, but helps with this data set
            .gradientNormalizationThreshold(0.5)
            .list()
                .layer(new LSTM.Builder().activation(Activation.TANH).nIn(2).nOut(10).build())
                .layer(new BatchNormalization.Builder().build())
                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX).nIn(10).nOut(numLabelClasses).build())
            .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        log.info("Starting training...");
        net.setListeners(new ScoreIterationListener(20));   //Print the score (loss function value) every 20 iterations

        int nEpochs = 10;
        net.fit(trainData, nEpochs);

        log.info("******SAVE TRAINED MODEL******");
        // Details
        // Where to save model
        File locationToSave = new File("trained_seq_model.zip");
        // boolean save Updater
        boolean saveUpdater = false;
        // ModelSerializer needs modelname, saveUpdater, Location
        ModelSerializer.writeModel(net,locationToSave,saveUpdater);



        log.info("----- Example Complete -----");

    }

}
