package me.systembug.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;
import me.systembug.device.pref.DevicesListPreference;

/**
 * 
 * Auxiliary class with several function blocks that deal with the problem,
 * That many modern Android devices have technically two SD cards:
 * <ul>
 * <li>The "old" memory, the Android as "external memory" or under
 * "mnt/sdcard" anspricht, ist dabei fest eingebaut; er hat zwar "sd" im Namen
 * und wird ?ber Methoden mit "external" im Namen angesprochen, aber
 * das stimmt ansich nicht: Dieser Speicher ist nicht wechselbar und technisch
 * nicht als echte physische SD-Karte implementiert.
 * <li>Der bei diesen Ger?ten zug?ngliche Slot f?r eine microSD-Karte wird 
 * nicht ?ber diese External-Methoden angesprochen. Bis Android 4.1 gibt es
 * keinen offiziellen Weg, drauf zuzugreifen. Daher liegt bei diesen Smartphones
 * die SD-Karte f?r Apps weitgehend brach. 
 * </ul>
 * <p>
 * Siehe dazu auch c't 22/12.
 * <p>Die Klasse selbst muss nicht instantiiert werden, sondern alle Methoden
 * sind static und beim Start der App wird automatisch {@link #rescanDevices()} 
 * aufgerufen, um die Liste der Devices zu erzeugen und somit eine echte
 * SD-Karte zu finden. Die Liste wird nicht automatisch aktualisiert, aber 
 * eine App kann rescanDevices() aufrufen. Auch ist ein BroadcastReceiver f?r 
 * automatische Updates implementiert, muss aber von der App
 * aufgerufen werden (siehe unten).
 * 
 * <h2>Die Funktionsbl?cke</h2>
 * <ol>
 * <li>In Version 1.2 der Library ist eine neue Zugriffsmethode eingebaut, die
 * das Einstellen des Speicherorts per PreferenceScreen erleichtert. Dazu bindet
 * man ein {@link DevicesListPreference} in seine XML-Datei ein, definiert
 * dort ?ber ein paar Parameter, welche Devices angezeigt werden sollen und
 * bekommt dann per statischer Methode 
 * {@link DevicesListPreference#getDevice(Context, android.content.SharedPreferences, String)}
 * das gespeicherte Device mitgeteilt. Im erweiterten {@link Device} gibt
 * es nun Methoden, die analog zu den Environment- oder Context-Methoden
 * Unterverzeichnisse erzeugen. Von den hier aufgef?hrten Methoden ben?tigt
 * man dann nur die f?r den BroadcastReceiver.
 * 
 * <li>Ob die bei vielen modernen Ger?ten (v.a. Tablets) vorhandene externe 
 * SD-Karte existiert, liefert {@link #isSecondaryExternalStorageAvailable()},
 * ob sie entfernbar ist, {@link #isSecondaryExternalStorageRemovable()}. 
 * 
 * <li>Um auf die Karte zugreifen zu k?nnen, sind von den "External"-Methoden 
 * (zum Zugriff auf die interne SD-Karte) ?quivalente mit "Secondary" im 
 * Namen vorhanden. Entsprechend {@link Environment} sind das 
 * {@link #getSecondaryExternalStorageDirectory()}, 
 * {@link #getSecondaryExternalStoragePublicDirectory(String)}, 
 * {@link #getSecondaryExternalStorageState()} 
 * und entsprechend {@link Context}
 * {@link #getSecondaryExternalCacheDir(Context)}, 
 * {@link #getSecondaryExternalFilesDir(Context, String)}.
 * 
 * <li>Um alles noch einen Schritt zu vereinfachen, gibt es den Methodensatz
 * auf Anregung von Sven Wiegand nochmal mit "Card" im Namen. Sie greifen
 * auf die externe SD-Karte zu und machen automatisch ein Fallback auf 
 *	/mnt/sdcard, falls keine externe vorhanden ist. Wenn ein sekund?rer Slot
 *	vorhanden ist, aber keine Karte eingesteckt, wird auch der Fallback
 *	benutzt. Ob die prim?re oder sekund?re Speicherkarte benutzt wird, l?sst 
 *	sich per {@link #isSecondaryExternalStorageAvailable()} herausfinden.
 *	<p>
 *	Die App muss nat?rlich ?berpr?fen, ob die Daten an der Speicherstelle
 *	noch vorhanden sind; das muss aber sowieso sein.
 *	<p>
 *	Etwas problematisch: Wenn die App gestartet wird, bevor eine sekund?re
 *	Speicherkarte eingelegt wird, greifen diese Methoden auf diese Karte zu
 *	und finden dort die Daten nicht, obwohl sie auf dem internen Speicher
 *	liegen. Als Abhilfe sollte die App sich den Speicherort evtl. abspeichern.
 *	<p>
 * Implementiert sind: {@link #getCardDirectory()}, 
 * {@link #getCardPublicDirectory(String)}, {@link #getCardState()} , 
 * {@link #getCardCacheDir(Context)}, {@link #getCardFilesDir(Context, String)}.
 * 
 * <li>Weil das Auffinden der SD-Karte versagen kann oder weil Anwender 
 * ausw?hlen k?nnen sollen, wo sie ihre Daten speichern, ist auch eine 
 * Durchsuchfunktion f?r alle extern anbindbaren Ger?te (diese Zweit-SD, 
 * aber auch USB-Ger?te oder Kartenleser) vorhanden. Eine App kann
 * damit eine Liste aller verf?gbaren Speichermedien (inklusive der 
 * internen SD-Karte) anzeigen, zusammen mit deren Kapazit?t und 
 * freien Speicherplatz: {@link #getDevices(String, boolean, boolean)}.
 * Die zur?ckgegebene Liste kann man dann ausgeben; in 
 * {@link Device} gibts Methoden f?r Pfad, Name und Gr??e der Devices.
 * 
 * <li>Zwei Hilfsmethoden, die die beide ab API9 und 13 in Environment 
 * vorhandenen Funktionen zur Analyse des Ger?ts auch unter API8 nutzbar 
 * machen, und die einige Besonderheiten von unseren Testger?ten 
 * ber?cksichtigen: {@link #isExternalStorageEmulated()} und 
 * {@link #isExternalStorageRemovable()}.
 * 
 * <li>Hilfsmethoden zum Zugriff auf /data, /mnt/sdcard und ggf. die
 * externe Karte per {@link Device}-Interface: {@link #getPrimaryExternalStorage()},
 * {@link #getSecondaryExternalStorage()} und {@link #getInternalStorage()}
 * </ol>
 * 
 * <h2>Aktualisierung der Ger?teliste</h2>
 * Falls eine App mitbekommen will, wenn Ger?te oder die SD-Karte
 * eingesteckt und entfernt werden, erstellt sie entweder einen eigenen
 * BroadcastReceiver oder (einfacher) ?bergibt {@link #registerRescanBroadcastReceiver()}
 * ein Runnable oder, falls man Zugriff auf den Intent haben m?chte, einen 
 * BroadcastReceiver.. 
 * <ul>
 * <li>Der eigene Receiver sollte in onReceive()  {@link #rescanDevices()} aufrufen. 
 * Die Methode {@link #getRescanIntentFilter()} erzeugt den richtigen
 * IntentFilter f?r den Receiver.
 * <li>Der in registerRescanBroadcastReceiver() automatisch erzeugte Receiver
 * ruft erst rescanDevices() und danach den ?bergebenen Runnable auf (der
 * auch null sein kann). registerReceiver wird auch automatisch aufgerufen,
 * aber an {@link Context#unregisterReceiver()} (?blicherweise in onDestroy())
 * muss man selbst denken.
 * </ul> 
 * <h2>Background</h2>
 * Diese Liste aller mountbaren Ger?te (wozu diese Zweit-SDs z?hlt) l?sst sich 
 * gl?cklicherweise bei allen bisher getesteten Ger?ten aus der Systemdatei 
 * /system/etc/vold.fstab auslesen, einer Konfigurationsdatei eines 
 * Linux-D?mons, der genau f?r das Einbinden dieser Ger?te zust?ndig ist. 
 * Es mag Custom-ROMs geben, wo diese Methode nicht funktioniert.
 * <p>
 * Der MountPoint f?r die zweite SD-Karte stand bei allen bisher getesteten 
 * Ger?ten direkt an erster Stelle dieser Datei, bei einigen nach /mnt/sdcard 
 * an zweiter Stelle. 
 * <p>
 * Andere Algorithmen zum Herausfinden des MountPoints sind noch nicht 
 * implementiert; das w?rde ich erst machen, wenn diese Methode hier bei 
 * einem Ger?t versagt. Denkbar w?re z.B. einfach eine Tabelle mit bekannten
 * MountPoints, die man der Reihe nach abklappert.
 * <p>
 *	Varianten des SD-Pfads sind:
 * <li>Asus Transformer		/Removable/MicroSD
 * <li>HTC Velocity LTE		/mnt/sdcard/ext_sd
 * <li>Huawei MediaPad		/mnt/external
 * <li>Intel Orange					/mnt/sdcard2
 * <li>LG Prada						/mnt/sdcard/_ExternalSD
 * <li>Motorola Razr			/mnt/sdcard-ext
 * <li>Motorola Razr i		/mnt/external1 (zeigt es sogar korrekt an!)
 * <li>Motorola Xoom			/mnt/external1
 * <li>Samsung Note			/mnt/sdcard/external_sd (und Pocket und Mini 2)
 * <li>Samsung Note II			/storage/extSdCard
 * <li>Samsung S3					/mnt/extSdCard
 *  <p>
 *  Einige Hersteller h?ngen die SD-Karte unter /mnt ein, andere in die 
 *  interne Karte /mnt/sdcard (was dazu f?hrt, dass einige Unterverzeichnisse
 *  von /mnt/sdcard gr??er sind als der gesamte interne Speicherbereich ;-), 
 *  wieder andere in ein anderes Root-Verzeichnis.
 *  
 *  @author J?rg Wirtgen (jow@ct.de)

 *  @version 1.4
 *  
 *  @version 1.5 - auch f?r API7; die Zugriffe hier auf {@link Context}.getXXXDir
 *  	finden nun ?ber die Methoden von mPrimary statt, was ein {@link DeviceExternal}
 *  	ist und bei Versionen vor API8 eine Emulation f?hrt ?hnlich der in 
 *  	{@link DeviceDiv} sowieso vorhandenen.
 */

public class Environment2  {
	private static final String TAG = "Environment2";
	private static final boolean DEBUG = true;
	
	private static ArrayList<DeviceDiv> mDeviceList = null;
	private static boolean mExternalEmulated = false;
	private static Device mInternal = null;
	protected static DeviceExternal mPrimary = null;
	private static DeviceDiv mSecondary = null;

	public final static String PATH_PREFIX = "/Android/data/";
	static {
		rescanDevices();
	}


	/**
	 * Ask if the second SD is present. The more accurate status can be
	 * Then be queried using {@link #getSecondaryExternalStorageState ()}.
	 * @return true, When a second SD is present and inserted,
	 *			false if not inserted or no slot present
	 */
	public static boolean isSecondaryExternalStorageAvailable() {
		return mSecondary!=null && mSecondary.isAvailable();
	}

	
	/**
	 * Indicates whether the second SD can be removed; Currently I know none
	 * Device, where the fixed were, so always true
	 * @return true
	 * @throws NoSecondaryStorageException If no two-SD is present
	 * @see #isSecondaryExternalStorageAvailable()
	 */
	public final static boolean isSecondaryExternalStorageRemovable() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		return true;
	}
	
	
	/**
	 * A pointer to the second SD, if found
	 * @return das Verzeichnis der Zwei-SD
	 * @throws NoSecondaryStorageException wenn keine Zwei-SD vorhanden oder nicht eingelegt
	 * @see #isSecondaryExternalStorageAvailable()
	 */
	public static File getSecondaryExternalStorageDirectory() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		return mSecondary.getFile(); 
	}

	
	/**
	 * Returns the status of a second SD card or throws an exception
	 * <p> A permission is required to write to the card:
	 * 		<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
	 * <p>
	 * @return One of the three states defined in Environment
	 * 	MEDIA_MOUNTED, _MOUNTED_READ_ONLY und _REMOVED
	 * @throws NoSecondaryStorageException If there is no second SD slot
	 * 
	 * @see #isSecondaryExternalStorageAvailable()
	 */
	public static String getSecondaryExternalStorageState() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		return mSecondary.getState();
	}

	
	/**
	 * Returns the public directories on the second SD, if used
	 * sie (wie die Environment-Methode) nicht an.
	 * @param s A string from Environment.DIRECTORY_xxx,
	 * 			Can not be null. (Also works with other path names and with nested ones)
	 * @return ein File dieses Verzeichnisses. Wenn Schreibzugriff gew?hrt,
	 * wird es angelegt, falls nicht vorhanden
	 * @throws NoSecondaryStorageException falls keine Zweit-SD vorhanden
	 */
	public static File getSecondaryExternalStoragePublicDirectory(String s) throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		if (s==null) throw new IllegalArgumentException("s darf nicht null sein");
		return mSecondary.getPublicDirectory(s);
	}
	
	
	/**
	 * Reconstruction of the context method getExternalFilesDir (String) with two differences:
	 * <ol>
	 * <li>You have to stop Context
	 * <li>The directory is not deleted during app uninstallation
	 * </ol>
	 * @param context The context of the app; To read the path name
	 * @param s A string from Environment.DIRECTORY_xxx, but can be another (nested) or null
	 * @return the directory. Is created if you have write access
	 * @throws NoSecondaryStorageException falls keine Zwei-SD vorhanden
	 */
	public static File getSecondaryExternalFilesDir(Context context, String s) throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		if (context==null) throw new IllegalArgumentException("context darf nicht null sein");
		return mSecondary.getFilesDir(context, s);
	}
	
	
	public static File getSecondaryExternalCacheDir(Context context) throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		if (context==null) throw new IllegalArgumentException("context darf nicht null sein");
		return mSecondary.getCacheDir(context);
	}
	

/*	
	 * Implementiert sind: {@link #getCardDirectory()}, 
	 * {@link #getCardPublicDirectory(String)}, {@link #getCardStorageState()} , 
	 * {@link #getCardCacheDir(Context)}, {@link #getCardFilesDir(Context, String)}.
*/
	public static File getCardDirectory() {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalStorageDirectory();} 
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return Environment.getExternalStorageDirectory();
	}

	public static File getCardPublicDirectory(String dir) {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalStoragePublicDirectory(dir);} 
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return mPrimary.getPublicDirectory(dir);
	}

	public static String getCardState() {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalStorageState();} 
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return Environment.getExternalStorageState();
	}

	public static File getCardCacheDir(Context ctx) {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalCacheDir(ctx);} 
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return mPrimary.getCacheDir(ctx);
	}

	public static File getCardFilesDir(Context ctx, String dir) {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalFilesDir(ctx, dir);} 
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return mPrimary.getFilesDir(ctx, dir);
	}


	/**
	 * Alternative zu {@code Environment#isExternalStorageEmulated() }, 
	 * die ab API8 funktioniert. Wenn true geliefert wird, handelt es sich
	 * um ein Ger?t mit "unified memory", bei dem /data und /mnt/sdcard
	 * auf denselben Speicherbereich zeigen. App2SD ist dann deaktiviert,
	 * und zum Berechnen des freien Speichers darf man nicht den der beiden
	 * Partitionen addieren, sondern nur einmal z?hlen. 
	 * 
	 * @return true, falls /mnt/sdcard und /data auf den gleichen Speicherbereich zeigen; 
	 * 	false, falls /mnt/sdcard einen eigenen (nicht notwendigerweise auf einer
	 * externen SD-Karte liegenden!) Speicherbereich beschreibt
	 * 
	 * @see #isExternalStorageRemovable()
	 */
	public static boolean isExternalStorageEmulated() {
		return mExternalEmulated; 
	}

	
	/**
	 * Alternative zu {@link Environment#isExternalStorageRemovable()}, 
	 * die ab API8 funktioniert. Achtung: Die Bedeutung ist eine subtil andere 
	 * als beim Original-Aufruf. Hier geht es (eher zu Hardware-Diagnosezwecken) 
	 * darum, ob /mnt/sdcard eine physische Karte ist, die der Nutzer 
	 * herausnehmen kann. Der Original-Aufruf liefert true, wenn es sein kann, 
	 * dass auf /mnt/sdcard nicht zugegriffen werden kann, was auch bei fest
	 * eingebauten Karten der Fall sein kann, und zwar wenn sie per USB
	 * an einen PC freigegeben werden k?nnen und w?hrenddessen nicht
	 * f?r Android im Zugriff stehen.
	 * 
	 * @return true, falls /mnt/sdcard auf einer entnehmbaren 
	 * physischen Speicherkarte liegt
	 * 	false, falls das ein fest verl?teter Speicher ist - das hei?t nicht, 
	 * dass immer auf den Speicher zugegriffen werden kann, ein 
	 * Status-Check muss dennoch stattfinden (anders als 
	 * bei Environment.isExternalStorageRemovable())
	 * 
	 * @see #isExternalStorageEmulated()
	 */
	public static boolean isExternalStorageRemovable() { 
		return mPrimary.isRemovable();
	}

	
	
	/**
	 * Hilfe zum Erstellen eines BroadcastReceivers: So muss der passende 
	 * IntentFilter aussehen, damit der Receiver alle ?nderungen mitbekommt.
	 * Wenn man einen eigenen Receiver programmiert statt 
	 * {@link Environment2#registerRescanBroadcastReceiver(Context, Runnable)}
	 * zu nutzen, sollte man dort {@link Environment2#rescanDevices()} aufrufen.
	 * @return einen IntentFilter, der auf alle Intents h?rt, die einen Hardware-
	 * 		und Kartenwechsel anzeigen
	 * @see IntentFilter
	 */
	public static IntentFilter getRescanIntentFilter() {
		if (mDeviceList==null) rescanDevices();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL); // rausgenommen
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED); // wieder eingesetzt
		filter.addAction(Intent.ACTION_MEDIA_REMOVED); // entnommen
		filter.addAction(Intent.ACTION_MEDIA_SHARED); // per USB am PC
		// geht ohne folgendes nicht, obwohl das in der Doku nicht so recht steht
		filter.addDataScheme("file"); 

		/*
		 * die folgenden waren zumindest bei den bisher mit USB getesteten 
		 * Ger?ten nicht notwendig, da diese auch bei USB-Sticks und externen 
		 * SD-Karten die ACTION_MEDIA-Intents abgefeuert haben
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		 */
		return filter;
	}
	
	
	/**
	 * BroadcastReceiver, der einen Rescan durchf?hrt und (als Callback) das 
	 * ?bergebene Runnable aufruft. Muss mit unregisterReceiver freigegeben 
	 * werden; daf?r ist der Aufrufer verantwortlich. Das Registrieren des
	 * Receivers wird hier schon durchgef?hrt (mit getRescanIntentFilter)
	 * <p>
	 * Das geht dann (z.B. in onCreate() ) so (mit {@code BroadcastReceiver mRescanReceiver;}) : <pre>
		mRescanReceiver = Environment2.registerRescanBroadcastReceiver(this, new Runnable() {
	 		public void run() {
	 			auszuf?hrende Befehle
	 		}
	 	});</pre>
	 * <p>
	 * und sp?ter (z.B. in onDestroy() ): {@code unregisterReceiver(mRescanReceiver);}
	 * <p>
	 * Der hier implementierte Receiver macht nichts anderes als {@link #rescanDevices() }
	 * und dann den Runnable aufzurufen.
	 * <p>
	 * TODO Problematisch ist, dass bei MEDIA_BAD_REMOVAL die Daten des f?lschlich
	 * 	entnommenen Sticks noch vorhanden sind.
	 * 
	 * @param context der Context, in dem registerReceiver aufgerufen wird
	 * @param r der Runnable, der bei jedem An- und Abmelden von Devices 
	 * 		ausgef?hrt wird; kann auch null sein
	 * @return der BroadcastReceiver, der sp?ter unregisterReceiver ?bergeben 
	 * 		werden muss. Registriert werden muss er nicht, das f?hrt die 
	 * 		Methode hier durch.
	 * @see #getRescanIntentFilter()
	 * @see BroadcastReceiver
	 */
	public static BroadcastReceiver registerRescanBroadcastReceiver(Context context, final Runnable r) {
		if (mDeviceList==null) rescanDevices();
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override public void onReceive(Context context, Intent intent) {
				if (DEBUG) Log.i(TAG, "Storage: "+intent.getAction()+"-"+intent.getData());
				updateDevices();
				if (r!=null) r.run();
			}
		};
		context.registerReceiver(br, getRescanIntentFilter());
		return br;
	}

	
	/**
	 * Wie {@link #registerRescanBroadcastReceiver(Context, Runnable)}, nur dass ein BroadcastReceiver
	 * ?bergeben werden muss, deren onReceive dann aufgerufen wird. Der Unterschied: Hier bekommt
	 * der Aufrufer den Intent mitgeteilt, beim anderen Aufruf nicht.
	 * 
	 * @param context der Context der App
	 * @param r der BroadcastReceiver, dessen onReceive() aufgerufen werden soll
	 * @return ein BroadcastReceiver (nicht der ?bergebene), der sp?ter dann unregisterReceiver
	 * 	?bergeben werden muss. Registriert werden muss er nicht, das f?hrt die Methode durch.
	 * @since 1.4
	 */
	public static BroadcastReceiver registerRescanBroadcastReceiver(Context context, final BroadcastReceiver r) {
		if (mDeviceList==null) rescanDevices();
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override public void onReceive(Context context, Intent intent) {
				if (DEBUG) Log.i(TAG, "Storage: "+intent.getAction()+"-"+intent.getData());
				updateDevices();
				if (r!=null) r.onReceive(context, intent);
			}
		};
		context.registerReceiver(br, getRescanIntentFilter());
		return br;
	}


	/**
	 * Scannt die Verf?gbarkeit aller Ger?te neu, ohne {@link #rescanDevices()} aufzurufen.
	 * 
	 * <p>Wird vom BroadcastReceiver aufgerufen. Falls die App einen eigenen
	 * BroadcastReceiver zum Erkennen von Wechseln bei Devices schreibt, 
	 * sollte in dessen onReceive() diese Methode aufgerufen werden.
	 * 
	 * @see Environment2#registerRescanBroadcastReceiver(Context, Runnable)
	 * @since 1.3
	 */
	public static void updateDevices() {
		for (Device i : mDeviceList) {i.updateState();}
		mPrimary.updateState();
	}

	
	/**
	 * Sucht das Ger?t nach internen und externen Speicherkarten und USB-Ger?ten
	 * ab. Wird automatisch beim App-Start aufgerufen (in einem static-initializer) und
	 * muss nach bisherigen Erkenntnissen nie von der App aufgerufen werden.
	 */
	@SuppressLint("NewApi")
	public static void rescanDevices() {
		mDeviceList = new ArrayList<DeviceDiv>(10);
		mPrimary = new DeviceExternal();

		// vold.fstab lesen; TODO bei Misserfolg eine andere Methode
		if (!scanVold("vold.fstab")) scanVold("vold.conf");

    	// zeigen /mnt/sdcard und /data auf denselben Speicher?
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    		mExternalEmulated = Environment.isExternalStorageEmulated();
    	} else {
    		// vor Honeycom gab es den unified memory noch nicht
    		mExternalEmulated = false; 
    	}

		// Pfad zur zweiten SD-Karte suchen; bisher nur Methode 1 implementiert
		// Methode 1: einfach der erste Eintrag in vold.fstab, ggf. um ein /mnt/sdcard-Doppel bereinigt
		// Methode 2: das erste mit "sd", falls nicht vorhanden das erste mit "ext"
		// Methode 3: das erste verf?gbare
		if (mDeviceList.size()==0) {
			mSecondary = null;
			// TODO Ger?te mit interner SD und Android 2 wie Nexus S
			// if (nexus) mPrimary.setRemovable(false);
		} else {
			mSecondary = mDeviceList.get(0);
			if (mSecondary.getName().contains("usb")) {
				// z.B. HTC One X+
				mSecondary = null;
			} else {
				// jau, SD gefunden
				mSecondary.setName("SD-Card");
				// Hack
				if (mPrimary.isRemovable()) Log.w(TAG, "isExternStorageRemovable overwrite (secondary sd found) auf false");
				mPrimary.setRemovable(false);
			}
		}
	}
	
	
	/**
	 * Die vold-Konfigurationsdatei auswerten, die ?blicherweise 
	 * in /system/etc/ liegt. 
	 * @param name ein String mit dem Dateinamen (vold.fstab oder vold.conf)
	 * @return true, wenn geklappt hat; false, wenn Datei nicht (vollst?ndig) 
	 * 		gelesen werden konnte. Falls false, werden die bisher gelesenen
	 * 		Devices nicht wieder gel?scht, sondern bleiben in der Liste 
	 * 		enthalten. Bisher ist mir aber noch kein Ger?t untergekommen,
	 * 		bei dem dieser Trick nicht funktioniert hat.
	 */
	private static boolean scanVold(String name) {
		String s, f;
		boolean prefixScan = true; // sdcard-Prefixes
		SimpleStringSplitter sp = new SimpleStringSplitter(' ');
    	try {
    		BufferedReader buf = new BufferedReader(new FileReader(Environment.getRootDirectory().getAbsolutePath()+"/etc/"+name), 2048);
    		s = buf.readLine();
    		while (s!=null) {
    			sp.setString(s.trim());
    			f = sp.next(); // dev_mount oder anderes
        		if ("dev_mount".equals(f)) {
        			DeviceDiv d = new DeviceDiv(sp);
        			
        			if (TextUtils.equals(mPrimary.getMountPoint(), d.getMountPoint())) {
        				// ein wenig Spezialkrams ?ber /mnt/sdcard herausfinden
        				
        				// wenn die Gingerbread-Funktion isExternalStorageRemovable nicht da ist, diesen Hinweis nutzen
        				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) 
        					mPrimary.setRemovable(true); 
        					// dann ist auch der Standard-Eintrag removable
        					// eigentlich reicht das hier nicht, denn die vold-Eintr?ge f?r die prim?re SD-Karte sind viel komplexer, 
        					// oft steht da was von non-removable. Doch diese ganzen propriet?ren Klamotten auszuwerden,
        					// w?re viel zu komplex. Ein gangbarer Kompromiss scheint zu sein, sich ab 2.3 einfach auf
        					// isExternalStorageRemovable zu verlassen, was schon oben in Device() gesetzt wird. Bei den
        					// bisher aufgetauchten Ger?ten mit 2.2 wiederum scheint der Hinweis in vold zu klappen.vccfg
        				
        				// z.B. Galaxy Note h?ngt "encryptable_nonremovable" an
        				while (sp.hasNext()) {
        					f = sp.next();
        					if (f.contains("nonremovable")) {
        						mPrimary.setRemovable(false);
        						Log.w(TAG, "isExternStorageRemovable overwrite ('nonremovable') auf false");
        					}
        				}
        				prefixScan = false;
        			} else 
        				// nur in Liste aufnehmen, falls nicht Dupe von /mnt/sdcard
        				mDeviceList.add(d);
        			
        		} else if (prefixScan) {
					// Further investigations only if before sdcard entry
					// something unclean, since it must actually occur in {}, which I am not checking here
        			if ("discard".equals(f)) {
        				// manche (Galaxy Note) schreiben "discard=disable" vor den sdcard-Eintrag.
        				sp.next(); // "="
        				f = sp.next();
        				if ("disable".equals(f)) {
        					mPrimary.setRemovable(false);
        					Log.w(TAG, "isExternStorageRemovable overwrite ('discard=disable') auf false");
        				} else if ("enable".equals(f)) {
							// ha, denkste ... so far I have found the entry only with two mobile phones, (Galaxy Note, Galaxy Mini 2), and
							// he did not vote *, but the cards were not removable.
							// mPrimary.mRemovable = true;
        					Log.w(TAG, "isExternStorageRemovable overwrite overwrite ('discard=enable'), bleibt auf "+mPrimary.isRemovable());
        				} else
        					Log.w(TAG, "disable-Eintrag unverst?ndlich: "+f);
        			}
        			
        		}
    			s = buf.readLine();
    		}
    		buf.close();
    		Log.v(TAG, name+" gelesen; Ger?te gefunden: "+mDeviceList.size());
    		return true;
    	} catch (Exception e) {
    		Log.e(TAG, "kann "+name+" nicht lesen: "+e.getMessage());
    		return false;
    	}
	}
	

	/**
	 * List of all found Removable devices. The list can be restricted by device name and other parameters.
	 * 
	 * @param key A string for restricting the list. Finds only the devices
	 * 			With the string in getName () or all, if null.
	 * @param available A boolean to restrict the list to existing ones
	 * 			(Plugged in) devices. False finds all, true only those that are plugged in.
	 * 			If false, device entries can be returned whose
	 * 			GetSize () object is null.
	 * @param intern A boolean that determines whether the internal memory (/ mnt / sdcard)
	 * 			Is included in the list (taking into account available, But not key).
	 * @param data a boolean that determines whether the data store (/ data) with
	 * 				Is added to the list
	 * @return an array containing all {@linkDevice} that match the search criteria
	 */
	public static Device[] getDevices(String key, boolean available, boolean intern, boolean data) {
		if (key!=null) key = key.toLowerCase();
		ArrayList<Device> temp = new ArrayList<Device>(mDeviceList.size()+2);
		if (data) temp.add(getInternalStorage());
		if (intern && ( !available || mPrimary.isAvailable())) temp.add(mPrimary);
		for (Device d : mDeviceList) {
			if ( ((key==null) || d.getName().toLowerCase().contains(key)) && (!available || d.isAvailable()) ) temp.add(d);
		}
		return temp.toArray(new Device[temp.size()]);
	}
	

	public static Device getPrimaryExternalStorage() {
		return mPrimary;
	}
	
	
	public static Device getSecondaryExternalStorage() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		return mSecondary;
	}
	
	
	public static Device getInternalStorage() {
		if (mInternal==null) mInternal = new DeviceIntern();
		return mInternal;
	}
	
	
}
