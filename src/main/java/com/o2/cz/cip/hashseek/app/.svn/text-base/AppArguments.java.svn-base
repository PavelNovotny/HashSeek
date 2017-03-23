package com.o2.cz.cip.hashseek.app;

import com.o2.cz.cip.hashseek.core.HashSeekConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Pavel
 * Date: 29.3.13 18:07
 */
public class AppArguments {

	private String dayFrom;
	private String daysToSeek;
	private Set<String> seekedStrings;
	private String resultFilePrefix;
	private Calendar dateSeekFrom = null;
	private Calendar dateSeekTo;
	//    private Calendar current;
	private boolean seekProd;
	private boolean seekPredprod;
	private boolean seekTest;
	private boolean runinfo;
	private boolean onlineEsbSeek;
	private boolean onlineBPMSeek;
    private boolean noeSeek;
    private boolean includeTimeLogs;

	private Set<String> datesToScan = new HashSet<String>();
    
    static Logger LOGGER= LoggerFactory.getLogger(AppArguments.class);
//    public AppArguments() {
//        this.datesToScan = new HashSet<String>();
//        this.current = Calendar.getInstance();
//        current.setTimeInMillis(System.currentTimeMillis());
//    }

	public String getDayFrom() {
		return dayFrom;
	}

	public void setDayFrom(String dayFrom) {
		this.dayFrom = dayFrom;
		computeDateSeekFrom();
	}

	public boolean isRuninfo() {
		return runinfo;
	}

	public void setRuninfo(boolean runinfo) {
		this.runinfo = runinfo;
	}

	public boolean isSeekProd() {
		return seekProd;
	}

	public void setSeekProd(boolean seekProd) {
		this.seekProd = seekProd;
	}

	public boolean isSeekPredprod() {
		return seekPredprod;
	}

	public void setSeekPredprod(boolean seekPredprod) {
		this.seekPredprod = seekPredprod;
	}

	public boolean isSeekTest() {
		return seekTest;
	}

	public void setSeekTest(boolean seekTest) {
		this.seekTest = seekTest;
	}

	private void fillDatesToScan() {
		if (dateSeekFrom != null && daysToSeek != null && daysToSeek.length() != 0) {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(dateSeekFrom.getTimeInMillis());
			datesToScan.clear();
			datesToScan.add(HashSeekConstants.dateString(c));
			int dayCount = Integer.valueOf(daysToSeek);
			for (int i = 1; i < dayCount; i++) {
				c.add(Calendar.DAY_OF_YEAR, 1);
				datesToScan.add(HashSeekConstants.dateString(c));
			}
		} else {
			HashSeekConstants.outPrintLine("fillDatesToScan: daysToSeek is empty or dateSeekFrom is empty!");
		}
//		for (String s : datesToScan) {
//			LOGGER.info(">> " + s);
//		}
	}

	public String getDateFrom() {
		return HashSeekConstants.dateString(dateSeekFrom);
	}

	public String getDateTo() {
		return HashSeekConstants.dateString(dateSeekTo);
	}

	//TODO test only
	public void setDateFrom(String dateFrom) {
		this.dateSeekFrom = Calendar.getInstance();
		//this.dateSeekFrom.setTimeInMillis(current.getTimeInMillis());
		dateSeekFrom.set(Calendar.YEAR, Integer.valueOf(dateFrom.substring(0, 4)));
		dateSeekFrom.set(Calendar.MONTH, Integer.valueOf(dateFrom.substring(4, 6)));
		dateSeekFrom.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dateFrom.substring(6, 8)));
	}

//    public void setDateTo(String dateTo) {
//        this.dateSeekTo = Calendar.getInstance();
//        this.dateSeekTo.setTimeInMillis(current.getTimeInMillis());
//        dateSeekTo.set(Calendar.YEAR, Integer.valueOf(dateTo.substring(0, 4)));
//        dateSeekTo.set(Calendar.MONTH, Integer.valueOf(dateTo.substring(4, 6)));
//        dateSeekTo.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dateTo.substring(6, 8)));
//        fillDatesToScan();
//    }

	public String getDaysToSeek() {
		return daysToSeek;
	}

	public void setDaysToSeek(String daysToSeek) {
		this.daysToSeek = daysToSeek;
		computeDateSeekTo();
		fillDatesToScan();
	}

	public Set<String> getSeekedStrings() {
		return seekedStrings;
	}

	public void setSeekedStrings(Set<String> seekedStrings) {
		this.seekedStrings = seekedStrings;
	}

	public String getResultFilePrefix() {
		return resultFilePrefix;
	}

	public void setResultFilePrefix(String resultFilePrefix) {
		this.resultFilePrefix = resultFilePrefix;
	}

	public Set<String> getDatesToScan() {
		return datesToScan;
	}

	private void computeDateSeekFrom() {

		//integer
		try {
			dateSeekFrom = Calendar.getInstance();
			dateSeekFrom.setTimeInMillis(System.currentTimeMillis());
			int dayToSeek = Integer.parseInt(dayFrom);
			if (dayToSeek > dateSeekFrom.get(Calendar.DAY_OF_MONTH)) {
				dateSeekFrom.add(Calendar.MONTH, -1);
			}
			dateSeekFrom.set(Calendar.DAY_OF_MONTH, dayToSeek);
			return;
		} catch (NumberFormatException e) {
			//ignore
		}

		//date
		try {
			dateSeekFrom = Calendar.getInstance();
			dateSeekFrom.setTimeInMillis(HashSeekConstants.dateFormat.parse(dayFrom).getTime());
		} catch (ParseException e) {
			HashSeekConstants.outPrintLine("computeDateSeekFrom: dayFrom is invalid!" + dayFrom);
		}
	}

	private void computeDateSeekTo() {
		if (daysToSeek.length() != 0 && dateSeekFrom != null) {
			dateSeekTo = Calendar.getInstance();
			dateSeekTo.setTimeInMillis(dateSeekFrom.getTimeInMillis());
			dateSeekTo.add(Calendar.DAY_OF_MONTH, Integer.valueOf(daysToSeek) - 1);
			LOGGER.info("computeDateSeekTo: " + HashSeekConstants.dateFormat.format(dateSeekTo.getTime()));
		} else {
			HashSeekConstants.outPrintLine("computeDateSeekTo: daysToSeek is empty or dateSeekFrom is empty!");
		}
	}

//    public static void main (String[] args) throws Exception {
//        AppArguments appArguments = new AppArguments();
////        appArguments.setDateFrom("20130329");
////        appArguments.getDateFrom();
//        appArguments.setDayFrom("18.1.2014");
//        appArguments.setDaysToSeek("2");
//        appArguments.fillDatesToScan();
//    }

	public boolean isOnlineEsbSeek() {
		return onlineEsbSeek;
	}

	public void setOnlineEsbSeek(boolean onlineEsbSeek) {
		this.onlineEsbSeek = onlineEsbSeek;
	}

	public boolean isOnlineBPMSeek() {
		return onlineBPMSeek;
	}

	public void setOnlineBPMSeek(boolean onlineBPMSeek) {
		this.onlineBPMSeek = onlineBPMSeek;
	}


	public Calendar getDateSeekFrom() {
		return dateSeekFrom;
	}

	public Calendar getDateSeekTo() {
		return dateSeekTo;
	}

    public boolean isIncludeTimeLogs() {
        return includeTimeLogs;
    }

    public void setIncludeTimeLogs(boolean includeTimeLogs) {
        this.includeTimeLogs = includeTimeLogs;
    }

    public boolean isNoeSeek() {
        return noeSeek;
    }

    public void setNoeSeek(boolean noeSeek) {
        this.noeSeek = noeSeek;
    }
}
