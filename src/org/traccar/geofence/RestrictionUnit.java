package org.traccar.geofence;

import org.traccar.model.Polygon;
import org.traccar.model.Position;

/**
 * Created by niko on 12/26/15.
 */
public class RestrictionUnit {

    private long polygonId;

    private Integer restrictionType;

    private boolean periodical;

    private Integer dayOfWeek;

    private Integer interval;

    public Boolean check(Polygon polygon, Position position) {
        return true;
    }
    public long getPolygonId() {
        return polygonId;
    }

    public void setPolygonId(long polygonId) {
        this.polygonId = polygonId;
    }

    public Integer getRestrictionType() {
        return restrictionType;
    }

    public void setRestrictionType(Integer restrictionType) {
        this.restrictionType = restrictionType;
    }

    public boolean isPeriodical() {
        return periodical;
    }

    public void setPeriodical(boolean periodical) {
        this.periodical = periodical;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }
}