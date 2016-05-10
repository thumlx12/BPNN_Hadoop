package StandAloneNN;

import FileIO.FileReadNWrite;
import Jampack.JampackException;
import NeuralNetwork.ArtificialNeuralNetwork;
import NeuralNetwork.NeuronLayer;

import java.io.IOException;
import java.util.Vector;

import static FileIO.FileReadNWrite.readTxtFile;

/**
 * Created by mlx on 5/8/16.
 */
public class SA_MOVLBP {
    public static void main(String[] args) throws JampackException, IOException {
        Vector<Double[]> TrainData = readTxtFile("/home/mlx/Documents/DataSet/StandAloneFuncTrainData_WithNoise");
        Vector<Double[]> TestData = readTxtFile("/home/mlx/Documents/DataSet/StandAloneFuncTestData");

        int InputNum = 1;
        int LayerNum = 3;
        int[] NumEachLayer = {8 , 10, 1};
        int[] IndexEachLayer = {1, 1, 3};
        double MSE_upbound = 0.001;

        double[][] ErrVec = new double[NumEachLayer[NumEachLayer.length - 1]][1];
        double[][] ForwardResult;

        ArtificialNeuralNetwork FinalANN = new ArtificialNeuralNetwork(InputNum, LayerNum, NumEachLayer, IndexEachLayer);
        ArtificialNeuralNetwork BatchStore=new ArtificialNeuralNetwork(FinalANN.getANN());
        ArtificialNeuralNetwork SynthesisUpdate=null;
        NeuronLayer[] LastUpdate=null;

        String[] RunningMessage=new String[10000];

        System.out.println("Ite\tMSE");
        RunningMessage[0]="Ite\tMSE";

        double ThisIteMSE=0.0;
        double LastIteMSE=0.0;

        double LearnRate=0.1;
        double Momentum=0.8;
        double eta=1.05;
        double rho=0.7;
        double ksi=0.04;

        for (int Ite = 0;Ite<10000 ; Ite++) {
            double SquareErr = 0.0;
            BatchStore.clearNetwork();
            for (int i = 0; i < TrainData.size(); i++) {
                Double[] temp = (TrainData.get(i));
                double[][] InputVec = new double[InputNum][1];
                for (int k = 0; k < temp.length - 1; k++) {
                    InputVec[k][0] = temp[k];
                }
                double Tag = temp[temp.length - 1];

                ForwardResult = FinalANN.getForwardResult(InputVec);
                ErrVec[0][0] = Tag - ForwardResult[0][0];
                NeuronLayer[] ThisUpdate=  FinalANN.getSDBackwardUpdates(ErrVec, LearnRate);
                if(i==0){
                    FinalANN.updateWeightNetwork(ThisUpdate);
                    LastUpdate=ThisUpdate;
                    BatchStore.updateWeightNetwork(ThisUpdate);
                }
                else{
                    SynthesisUpdate=ArtificialNeuralNetwork.addTwoANN(ArtificialNeuralNetwork.multiplyNeuronLayers(LastUpdate,Momentum),ArtificialNeuralNetwork.multiplyNeuronLayers(ThisUpdate,1-Momentum));
                    FinalANN.updateWeightNetwork(SynthesisUpdate.getANN());
                    LastUpdate=SynthesisUpdate.getANN();
                    BatchStore.updateWeightNetwork(SynthesisUpdate);
                }
            }

            for (int i = 0; i < TrainData.size(); i++) {
                Double[] temp = (TrainData.get(i));
                double[][] InputVec = new double[InputNum][1];
                for (int k = 0; k < temp.length - 1; k++) {
                    InputVec[k][0] = temp[k];
                }
                double Tag = temp[temp.length - 1];
                ForwardResult = FinalANN.getForwardResult(InputVec);
                ErrVec[0][0] = Tag - ForwardResult[0][0];
                SquareErr += ErrVec[0][0] * ErrVec[0][0];
            }
            ThisIteMSE=SquareErr / TrainData.size();
            if(Ite==0){
                LastIteMSE=ThisIteMSE;
            }
            else{
                double MSE_VaryRate=(ThisIteMSE-LastIteMSE)/LastIteMSE;
                if(MSE_VaryRate>ksi){
                    FinalANN.updateWeightNetwork(ArtificialNeuralNetwork.multiplyNeuronLayers(BatchStore.getANN(),-1));
                    LearnRate*=rho;
                    Momentum=0;
                }
                else if(MSE_VaryRate<0){
                    LearnRate*=eta;
                    Momentum=0.8;
                    LastIteMSE=ThisIteMSE;
                }
                else{
                    Momentum=0.8;
                    LastIteMSE=ThisIteMSE;
                }
            }

            System.out.println(Ite + "\t" + (ThisIteMSE));
            RunningMessage[Ite] = String.valueOf(Ite) + "\t" + String.valueOf(SquareErr / TrainData.size());
            if (ThisIteMSE < MSE_upbound) {
                break;
            }
        }
        FileReadNWrite.LocalWriteFile("/home/mlx/Documents/ProcessResult/MOVLBP_IterationResult3", RunningMessage);

        String[] TestResult=new String[TestData.size()];
        double[][] TestForwardResult;
        double TestErrSum = 0.0;
        for (int i = 0; i < TestData.size(); i++) {
            Double[] temp = (TestData.get(i));
            double[][] InputVec = new double[InputNum][1];
            for (int k = 0; k < temp.length - 1; k++) {
                InputVec[k][0] = temp[k];
            }
            double Tag = temp[temp.length - 1];

            TestForwardResult = FinalANN.getForwardResult(InputVec);
            System.out.println(TestForwardResult[0][0]);
            TestResult[i]=String.valueOf(TestForwardResult[0][0]);
            TestErrSum += (Tag - TestForwardResult[0][0]) * (Tag - TestForwardResult[0][0]);
        }
        TestErrSum /= TestData.size();
        System.out.println("\n" + String.valueOf(TestErrSum));

        String[] SaveANN = FinalANN.saveANN();
        FileReadNWrite.LocalWriteFile("/home/mlx/Documents/ResultANN/MOVLBP_FinalANN", SaveANN);
        FileReadNWrite.LocalWriteFile("/home/mlx/Documents/ProcessResult/MOVLBP_PredictResult3", TestResult);

    }
}