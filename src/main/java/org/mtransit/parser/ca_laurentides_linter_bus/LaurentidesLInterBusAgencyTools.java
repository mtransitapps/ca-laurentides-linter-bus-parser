package org.mtransit.parser.ca_laurentides_linter_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://www.transportlaurentides.ca/wp-content/uploads/gtfs/taclgtfs.zip
public class LaurentidesLInterBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new LaurentidesLInterBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "L'Inter (TaCL)";
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
		if (!CharUtils.isDigitsOnly(routeId)) {
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
	public boolean directionSplitterEnabled() {
		return true;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
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
		final String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0 & CharUtils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		final Matcher matcher = DIGITS.matcher(stopCode);
		if (matcher.find()) {
			final int digits = Integer.parseInt(matcher.group());
			final int stopId;
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
