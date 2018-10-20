/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.support.v18.scanner;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

/**
 * ScanResult for Bluetooth LE scan.
 */
@SuppressWarnings({"WeakerAccess", "unused", "DeprecatedIsStillUsed"})
public final class ScanResult implements Parcelable {

	/**
	 * For chained advertisements, indicates that the data contained in this
	 * scan result is complete.
	 */
	public static final int DATA_COMPLETE = 0x00;

	/**
	 * For chained advertisements, indicates that the controller was
	 * unable to receive all chained packets and the scan result contains
	 * incomplete truncated data.
	 */
	public static final int DATA_TRUNCATED = 0x02;

	/**
	 * Indicates that the secondary physical layer was not used.
	 */
	public static final int PHY_UNUSED = 0x00;

	/**
	 * Advertising Set ID is not present in the packet.
	 */
	public static final int SID_NOT_PRESENT = 0xFF;

	/**
	 * TX power is not present in the packet.
	 */
	public static final int TX_POWER_NOT_PRESENT = 0x7F;

	/**
	 * Periodic advertising interval is not present in the packet.
	 */
	public static final int PERIODIC_INTERVAL_NOT_PRESENT = 0x00;

	/**
	 * Mask for checking whether event type represents legacy advertisement.
	 */
	static final int ET_LEGACY_MASK = 0x10;

	/**
	 * Mask for checking whether event type represents connectable advertisement.
	 */
	static final int ET_CONNECTABLE_MASK = 0x01;

	// Remote Bluetooth device.
	private BluetoothDevice mDevice;

	// Scan record, including advertising data and scan response data.
	@Nullable
	private ScanRecord mScanRecord;

	// Received signal strength.
	private int mRssi;

	// Device timestamp when the result was last seen.
	private long mTimestampNanos;

	private int mEventType;
	private int mPrimaryPhy;
	private int mSecondaryPhy;
	private int mAdvertisingSid;
	private int mTxPower;
	private int mPeriodicAdvertisingInterval;

	/**
	 * Constructs a new ScanResult.
	 *
	 * @param device Remote Bluetooth device found.
	 * @param scanRecord Scan record including both advertising data and scan response data.
	 * @param rssi Received signal strength.
	 * @param timestampNanos Timestamp at which the scan result was observed.
	 * @deprecated use {@link #ScanResult(BluetoothDevice, int, int, int, int, int, int, int, ScanRecord, long)}
	 */
	@SuppressWarnings("PointlessBitwiseExpression")
	public ScanResult(BluetoothDevice device, @Nullable ScanRecord scanRecord, int rssi,
					  long timestampNanos) {
		mDevice = device;
		mScanRecord = scanRecord;
		mRssi = rssi;
		mTimestampNanos = timestampNanos;
		mEventType = (DATA_COMPLETE << 5) | ET_LEGACY_MASK | ET_CONNECTABLE_MASK;
		mPrimaryPhy = 1; // BluetoothDevice.PHY_LE_1M;
		mSecondaryPhy = PHY_UNUSED;
		mAdvertisingSid = SID_NOT_PRESENT;
		mTxPower = 127;
		mPeriodicAdvertisingInterval = 0;
	}

	/**
	 * Constructs a new ScanResult.
	 *
	 * @param device Remote Bluetooth device found.
	 * @param eventType Event type.
	 * @param primaryPhy Primary advertising phy.
	 * @param secondaryPhy Secondary advertising phy.
	 * @param advertisingSid Advertising set ID.
	 * @param txPower Transmit power.
	 * @param rssi Received signal strength.
	 * @param periodicAdvertisingInterval Periodic advertising interval.
	 * @param scanRecord Scan record including both advertising data and scan response data.
	 * @param timestampNanos Timestamp at which the scan result was observed.
	 */
	public ScanResult(BluetoothDevice device, int eventType, int primaryPhy, int secondaryPhy,
					  int advertisingSid, int txPower, int rssi, int periodicAdvertisingInterval,
					  @Nullable ScanRecord scanRecord, long timestampNanos) {
		mDevice = device;
		mEventType = eventType;
		mPrimaryPhy = primaryPhy;
		mSecondaryPhy = secondaryPhy;
		mAdvertisingSid = advertisingSid;
		mTxPower = txPower;
		mRssi = rssi;
		mPeriodicAdvertisingInterval = periodicAdvertisingInterval;
		mScanRecord = scanRecord;
		mTimestampNanos = timestampNanos;
	}

	private ScanResult(Parcel in) {
		readFromParcel(in);
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (mDevice != null) {
			dest.writeInt(1);
			mDevice.writeToParcel(dest, flags);
		} else {
			dest.writeInt(0);
		}
		if (mScanRecord != null) {
			dest.writeInt(1);
			dest.writeByteArray(mScanRecord.getBytes());
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(mRssi);
		dest.writeLong(mTimestampNanos);
		dest.writeInt(mEventType);
		dest.writeInt(mPrimaryPhy);
		dest.writeInt(mSecondaryPhy);
		dest.writeInt(mAdvertisingSid);
		dest.writeInt(mTxPower);
		dest.writeInt(mPeriodicAdvertisingInterval);
	}

	private void readFromParcel(Parcel in) {
		if (in.readInt() == 1) {
			mDevice = BluetoothDevice.CREATOR.createFromParcel(in);
		}
		if (in.readInt() == 1) {
			mScanRecord = ScanRecord.parseFromBytes(in.createByteArray());
		}
		mRssi = in.readInt();
		mTimestampNanos = in.readLong();
		mEventType = in.readInt();
		mPrimaryPhy = in.readInt();
		mSecondaryPhy = in.readInt();
		mAdvertisingSid = in.readInt();
		mTxPower = in.readInt();
		mPeriodicAdvertisingInterval = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Returns the remote Bluetooth device identified by the Bluetooth device address.
	 */
	public BluetoothDevice getDevice() {
		return mDevice;
	}

	/**
	 * Returns the scan record, which is a combination of advertisement and scan response.
	 */
	@Nullable
	public ScanRecord getScanRecord() {
		return mScanRecord;
	}

	/**
	 * Returns the received signal strength in dBm. The valid range is [-127, 126].
	 */
	public int getRssi() {
		return mRssi;
	}

	/**
	 * Returns timestamp since boot when the scan record was observed.
	 */
	public long getTimestampNanos() {
		return mTimestampNanos;
	}

	/**
	 * Returns true if this object represents legacy scan result.
	 * Legacy scan results do not contain advanced advertising information
	 * as specified in the Bluetooth Core Specification v5.
	 */
	public boolean isLegacy() {
		return (mEventType & ET_LEGACY_MASK) != 0;
	}

	/**
	 * Returns true if this object represents connectable scan result.
	 */
	public boolean isConnectable() {
		return (mEventType & ET_CONNECTABLE_MASK) != 0;
	}

	/**
	 * Returns the data status.
	 * Can be one of {@link ScanResult#DATA_COMPLETE} or
	 * {@link ScanResult#DATA_TRUNCATED}.
	 */
	public int getDataStatus() {
		// return bit 5 and 6
		return (mEventType >> 5) & 0x03;
	}

	/**
	 * Returns the primary Physical Layer
	 * on which this advertisement was received.
	 * Can be one of {@link BluetoothDevice#PHY_LE_1M} or
	 * {@link BluetoothDevice#PHY_LE_CODED}.
	 */
	public int getPrimaryPhy() { return mPrimaryPhy; }

	/**
	 * Returns the secondary Physical Layer
	 * on which this advertisement was received.
	 * Can be one of {@link BluetoothDevice#PHY_LE_1M},
	 * {@link BluetoothDevice#PHY_LE_2M}, {@link BluetoothDevice#PHY_LE_CODED}
	 * or {@link ScanResult#PHY_UNUSED} - if the advertisement
	 * was not received on a secondary physical channel.
	 */
	public int getSecondaryPhy() { return mSecondaryPhy; }

	/**
	 * Returns the advertising set id.
	 * May return {@link ScanResult#SID_NOT_PRESENT} if
	 * no set id was is present.
	 */
	public int getAdvertisingSid() { return mAdvertisingSid; }

	/**
	 * Returns the transmit power in dBm.
	 * Valid range is [-127, 126]. A value of {@link ScanResult#TX_POWER_NOT_PRESENT}
	 * indicates that the TX power is not present.
	 */
	public int getTxPower() { return mTxPower; }

	/**
	 * Returns the periodic advertising interval in units of 1.25ms.
	 * Valid range is 6 (7.5ms) to 65536 (81918.75ms). A value of
	 * {@link ScanResult#PERIODIC_INTERVAL_NOT_PRESENT} means periodic
	 * advertising interval is not present.
	 */
	public int getPeriodicAdvertisingInterval() {
		return mPeriodicAdvertisingInterval;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mDevice, mRssi, mScanRecord, mTimestampNanos,
				mEventType, mPrimaryPhy, mSecondaryPhy,
				mAdvertisingSid, mTxPower,
				mPeriodicAdvertisingInterval);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ScanResult other = (ScanResult) obj;
		return Objects.equals(mDevice, other.mDevice) && (mRssi == other.mRssi) &&
				Objects.equals(mScanRecord, other.mScanRecord) &&
				(mTimestampNanos == other.mTimestampNanos) &&
				mEventType == other.mEventType &&
				mPrimaryPhy == other.mPrimaryPhy &&
				mSecondaryPhy == other.mSecondaryPhy &&
				mAdvertisingSid == other.mAdvertisingSid &&
				mTxPower == other.mTxPower &&
				mPeriodicAdvertisingInterval == other.mPeriodicAdvertisingInterval;
	}

	@Override
	public String toString() {
		return "ScanResult{" + "device=" + mDevice + ", scanRecord=" +
				Objects.toString(mScanRecord) + ", rssi=" + mRssi +
				", timestampNanos=" + mTimestampNanos + ", eventType=" + mEventType +
				", primaryPhy=" + mPrimaryPhy + ", secondaryPhy=" + mSecondaryPhy +
				", advertisingSid=" + mAdvertisingSid + ", txPower=" + mTxPower +
				", periodicAdvertisingInterval=" + mPeriodicAdvertisingInterval + '}';
	}

	public static final Parcelable.Creator<ScanResult> CREATOR = new Creator<ScanResult>() {
		@Override
		public ScanResult createFromParcel(Parcel source) {
			return new ScanResult(source);
		}

		@Override
		public ScanResult[] newArray(int size) {
			return new ScanResult[size];
		}
	};

}
