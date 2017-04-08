# StorageDevice

List all attached devices.

# Gradle

```Gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}


	dependencies {
    	        compile 'com.github.systembugtj:storagedevice:1.0.0'
    	}
```

# Usage

```Java
    Device[] device = Environment2.getDevices(null, true, true, false);
```
