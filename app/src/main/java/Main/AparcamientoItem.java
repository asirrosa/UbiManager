package Main;

public class AparcamientoItem {
    private String fechaHora;
    private String ubicacion;
    private Double lat;
    private Double lon;

    public AparcamientoItem(String fechaHora, String ubicacion, Double lat, Double lon) {
        this.fechaHora = fechaHora;
        this.ubicacion = ubicacion;
        this.lat = lat;
        this.lon = lon;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }
}

