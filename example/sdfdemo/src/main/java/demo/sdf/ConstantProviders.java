package demo.sdf;

import com.simplj.di.annotations.Constant;

public class ConstantProviders {
    @Constant(id = "files")
    public String path1() {
        return "./files/";
    }
    @Constant(id = "objects")
    public String path2() {
        return "./objects/";
    }

    @Constant(id = "fullDateTime")
    public String pattern1() {
        return "yyyyMMdd_HHmmssSSS";
    }

    @Constant(id = "shortDateTime")
    public String pattern2() {
        return "MMdd_HHmmss";
    }
}
