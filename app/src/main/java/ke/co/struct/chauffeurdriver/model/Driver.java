package ke.co.struct.chauffeurdriver.model;

public class Driver {

    private String name, phone, carType, carimg,licPlate, ProfileImageUrl;

    public Driver() {
    }

    public Driver(String name, String phone, String carType, String carimg, String licPlate, String profileImageUrl) {
        this.name = name;
        this.phone = phone;
        this.carType = carType;
        this.carimg = carimg;
        this.licPlate = licPlate;
        ProfileImageUrl = profileImageUrl;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getCarType() {
        return carType;
    }

    public String getCarimg() {
        return carimg;
    }

    public String getLicPlate() {
        return licPlate;
    }

    public String getProfileImageUrl() {
        return ProfileImageUrl;
    }
}
