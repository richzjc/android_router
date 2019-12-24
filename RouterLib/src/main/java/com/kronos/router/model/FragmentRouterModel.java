package com.kronos.router.model;

public class FragmentRouterModel {
    public String fragmentRouterUrl;
    public String path;
    private StringBuilder builder;

    private FragmentRouterModel(){
        builder = new StringBuilder();
    }

   public static class Builder{
        FragmentRouterModel model;

        public Builder nextFragmentRouter(String fragmentRouter){
            if(model == null) {
                model = new FragmentRouterModel();
            }
            model.builder.append(fragmentRouter).append(",");
            model.fragmentRouterUrl = fragmentRouter;
            return this;
        }

        public FragmentRouterModel build(){
            if(model == null) {
                model = new FragmentRouterModel();
            }
            String tempPath = model.builder.toString();
            if(tempPath.endsWith(",")){
                model.path = tempPath.substring(0, tempPath.length() - 1);
            }else{
                model.path = tempPath;
            }
            return model;
        }
    }
}
