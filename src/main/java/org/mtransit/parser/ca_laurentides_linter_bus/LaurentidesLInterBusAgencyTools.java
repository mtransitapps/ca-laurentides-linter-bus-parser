package org.mtransit.parser.ca_laurentides_linter_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://www.transportlaurentides.ca/wp-content/uploads/gtfs/taclgtfs.zip
public class LaurentidesLInterBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-laurentides-linter-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LaurentidesLInterBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating L'Inter (TaCL) bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating L'Inter (TaCL) bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		//noinspection deprecation
		final String routeId = gRoute.getRouteId();
		if (!Utils.isDigitsOnly(routeId)) {
			if ("ZCN".equals(routeId) //
					|| "ZCS".equals(routeId)) {
				return 1_003L;
			} else if ("ZNN".equals(routeId) //
					|| "ZNS".equals(routeId)) {
				return 1_014L;
			}
			throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
		}
		return super.getRouteId(gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		//noinspection deprecation
		final String routeId = gRoute.getRouteId();
		if ("ZCN".equals(routeId) //
				|| "ZCS".equals(routeId)) {
			return "ZC";
		} else if ("ZNN".equals(routeId) //
				|| "ZNS".equals(routeId)) {
			return "ZN";
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	@Override
	public boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge) {
		if (mRoute.getId() == 1_003L) {
			mRoute.setLongName("Inter Centre");
			return true;
		} else if (mRoute.getId() == 1_014L) {
			mRoute.setLongName("Inter Nord");
			return true;
		}
		return super.mergeRouteLongName(mRoute, mRouteToMerge);
	}

	private static final String AGENCY_COLOR = "E76525"; // ORANGE (from web site)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		final String tripHeadsign = gTrip.getTripHeadsignOrDefault();
		if (gTrip.getDirectionId() == null) {
			if (tripHeadsign.endsWith(" (Sud)")) {
				mTrip.setHeadsignString( //
						cleanTripHeadsign(tripHeadsign), //
						MDirectionType.SOUTH.intValue());
				return;
			}
			if (tripHeadsign.endsWith(" (Nord)")) {
				mTrip.setHeadsignString( //
						cleanTripHeadsign(tripHeadsign), //
						MDirectionType.NORTH.intValue());
				return;
			}
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(tripHeadsign),
				gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId() // TODO gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // DISABLED because direction_id NOT provided (& 2 routes = 1 route w/ 2 directions)
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.POINT.matcher(tripHeadsign).replaceAll(CleanUtils.POINT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(Locale.FRENCH, tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.cleanBounds(Locale.FRENCH, gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0 & Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		Matcher matcher = DIGITS.matcher(stopCode);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopCode.endsWith("N")) {
				stopId = 140_000;
			} else {
				if (stopId1.endsWith("S")) {
					stopId = 190_000;
				} else {
					throw new MTLog.Fatal("Stop doesn't have an ID (end with) %s!", gStop);
				}
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
