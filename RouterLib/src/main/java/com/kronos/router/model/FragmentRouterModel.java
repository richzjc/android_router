package com.kronos.router.model;

public class FragmentRouterModel {
    public String fragmentRouterUrl;
    public FragmentRouterModel next;

    class Builder{
        FragmentRouterModel model;

        public Builder nextFragmentRouter(String fragmentRouter){
            if(model == null) {
                model = new FragmentRouterModel();
                model.fragmentRouterUrl = fragmentRouter;
            }else{
                FragmentRouterModel tempModel = model;
                while (tempModel.next != null){
                    tempModel = tempModel.next;
                }

                FragmentRouterModel nextModel = new FragmentRouterModel();
                nextModel.fragmentRouterUrl = fragmentRouter;
                tempModel.next = nextModel;
            }
            return this;
        }

        public FragmentRouterModel build(){
            if(model == null) {
                model = new FragmentRouterModel();
            }
            return model;
        }
    }
}
