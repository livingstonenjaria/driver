package ke.co.struct.chauffeurdriver.model;

public class RequestInfo {
    private Double destinationLat,destinationLng;
    private String paymentMethod,riderDestination,riderLocation;

    public RequestInfo(Double destinationLat, Double destinationLng, String paymentMethod, String riderDestination, String riderLocation) {
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.paymentMethod = paymentMethod;
        this.riderDestination = riderDestination;
        this.riderLocation = riderLocation;
    }

    public Double getDestinationLat() {
        return destinationLat;
    }

    public Double getDestinationLng() {
        return destinationLng;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getRiderDestination() {
        return riderDestination;
    }

    public String getRiderLocation() {
        return riderLocation;
    }
}
