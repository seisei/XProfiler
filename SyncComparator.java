package application;

import java.util.Comparator;

public class SyncComparator implements Comparator<Sync> {

	@Override
	public int compare(Sync o1, Sync o2) {
		long result =  o1.startTime - o2.startTime;
		if (result < 0) {
			return -1;
		} else if (result == 0) {
			return 0;
		} else {
			return 1;
		}
	}

}
