package features;

public interface FeaturesEvaluator {
    public int[] getFeatures(int state,int action) throws Exception;
    int getFeaturesN();
}
