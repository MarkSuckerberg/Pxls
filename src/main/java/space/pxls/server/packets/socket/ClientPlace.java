package space.pxls.server.packets.socket;

import space.pxls.App;

public class ClientPlace {
    public String type;
    public Integer x;
    public Integer y;
    public Integer color;

    public ClientPlace(String type, Integer x, Integer y, Integer color) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getColor() {
        if (color == 0xFE) {
            color = ((int)App.getDefaultPixel(x, y));
        }

        return color;
    }
}
