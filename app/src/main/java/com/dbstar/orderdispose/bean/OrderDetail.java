package com.dbstar.orderdispose.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by wh on 2016/12/26.
 */
public class OrderDetail {


    /**
     * status : {"code":"1","msg":"成功"}
     * data : [{"buynum":"2","money":"20","goodsName":"意大利面"},{"buynum":"1","money":"10","goodsName":"意大利面"}]
     */

    private StatusBean status;
    private List<OrderDetailBean> data;

    public StatusBean getStatus() {
        return status;
    }

    public void setStatus(StatusBean status) {
        this.status = status;
    }

    public List<OrderDetailBean> getData() {
        return data;
    }

    public void setData(List<OrderDetailBean> data) {
        this.data = data;
    }

    public int getTotalCount() {
        int count = 0;
        for (OrderDetailBean bean : data) {
            if (bean.getBuynum() == null) {
                count = 1;
                break;
            }
            if (bean.getBuynum()!=null && bean.getBuynum().matches("[0-9]+")) {
                count += Integer.valueOf(bean.getBuynum());
            }
        }
        return count;
    }

    public int getTotalMoney() {
        int money = 0;
        for (OrderDetailBean bean : data) {
            if (bean.getMoney()!=null && bean.getMoney().matches("[0-9]+")) {
                money += Integer.valueOf(bean.getMoney());
            }
        }
        return money;
    }

    public String getOrderType() {
        String flag = null;
        for (OrderDetailBean bean : data) {
            //取订详情列表 第一个物品 所标识的 订单类型 作为整个订单的类型
            flag = bean.getFlag();
            break;
        }

        return flag;
    }


    public static class StatusBean {
        /**
         * code : 1
         * msg : 成功
         */

        private String code;
        private String msg;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

    public static class OrderDetailBean {
        /**
         * buynum : 2
         * money : 20
         * goodsName : 意大利面
         */

        private String buynum;
        private String money;
        @SerializedName(value = "goodsName", alternate = {"itemName"})
        private String goodsName;
        private String flag;


        public String getFlag() {
            return flag;
        }

        public void setFlag(String flag) {
            this.flag = flag;
        }

        public String getBuynum() {
            if (this.buynum == null) {
                this.buynum = "1";
            }
            return buynum;
        }

        public void setBuynum(String buynum) {
            this.buynum = buynum;
        }

        public String getMoney() {
            return money;
        }

        public void setMoney(String money) {
            this.money = money;
        }

        public String getGoodsName() {
            return goodsName;
        }

        public void setGoodsName(String goodsName) {
            this.goodsName = goodsName;
        }

        public int getPrice() {
            int price = 0;
            int m = 0, c = 1;
            if (getBuynum() == null) {
                return Integer.valueOf(getMoney());
            }
            if (getMoney()!=null && getMoney().matches("[0-9]+") && getBuynum().matches("[0-9]+")) {
                m += Integer.valueOf(getMoney());
                c = Integer.valueOf(getBuynum());
                if (c > 0) {
                    price += m / c;
                }
            }
            return price;
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
