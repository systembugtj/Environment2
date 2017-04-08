package me.systembug.device;

import java.io.File;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.StatFs;
import android.util.Pair;

/**
 * Miniklasse zum Ermitteln einer Partitions-Gr??e, die auch mit alten Android-Versionen 
 * zusammen klappt und ab Gingerbread die File-Methoden benutzt. Sie nutzt ein {@link Pair} zum 
 * Speichern der Informationen, wobei gilt:
 * <ul>
 * <li>first: freier Speicher (free), ermittelt per getUsableSpace
 * <li>secondary: Gesamtspeicher (size), ermittelt per getTotalSpace
 * </ul>
 * Der Constructor der Klasse ist privat, erzeugt wird ein Objekt ?ber die statische
 * Funktion getSpace(File).
 * 
 * @see Environment2
 * @author	J?rg Wirtgen (jow@ct.de)
 * @version	1.0 (3. Okt 12)
 */
public class Size extends Pair<Long,Long> {
	
	private Size(long free, long size) { super(free, size); }

	/**
	 * Versucht zu erraten, wie gro? das Speichermedium ist, auf dem das Size-Objekt
	 * liegt. Liefert nur einen sinnvollen Wert, wenn auf dem Speichermedium au?er 
	 * dem Size-Objekt entweder keine oder nur kleine andere Partitionen liegen.
	 * 
	 * @return die n?chsth?here Zweierpotenz ?ber der ermittelten Gr??e (secondary),
	 * 		also bsp. 16 GB f?r 14 GB. Falls second 0 ist, kommt 1 zur?ck.
	 */
	public long guessSize() {
		if (second==0) return 0;
		long g;
		if (second>1024*1024*1024) g = 1024*1024*1024;
		else if (second>1024*1024) g = 1024*1024;
		else g = 1;
		while (second>g) g *= 2;
		return g;
	}

	
	/**
	 * Ermittelt die Gr??e und den freien Speicher des ?bergebenen {@link File}. Ab 
	 * Android 2.3 werden dazu die File-Methoden {@link File#getUsableSpace()} und 
	 * {@link File#getTotalSpace()} benutzt, bei ?lteren Android-Versionen wird ein
	 * Hilfsobjekt {@link StatFs} erzeugt.
	 * 
	 * @param f das Verzeichnis, dessen Gr??e ermittelt werden soll
	 * @return ein {@link Pair}, das Gr??e und freier Speicher der Partition enth?lt, 
	 * 		auf die f zeigt oder (0,0), falls dabei ein Fehler aufgetreten ist oder f null ist.
	 */
	@SuppressLint("NewApi")
	public static Size getSpace(File f) {
		if (f!=null) try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
				// Gingerbread hat Gr??e/freier Platz im File
				return new Size(f.getUsableSpace(), f.getTotalSpace());
			} else {
				// vor Gingerbread muss der StatFs-Krams ran; wichtig ist die long-Wandlung
				StatFs fs = new StatFs(f.getAbsolutePath());
				return new Size((long)fs.getAvailableBlocks()*fs.getBlockSize(), (long)fs.getBlockCount()*fs.getBlockSize());
			}
		} catch (Exception e) { }
		return new Size((long)0, (long)0);
	}
}