package com.v6ak.ia158.legway.client

import org.scaloid.common._
import scala.collection.JavaConversions._
import android.widget.{AdapterView, ArrayAdapter}
import android.view.{View, Window}
import android.bluetooth.{BluetoothDevice, BluetoothAdapter}
import android.content.{IntentFilter, Intent, Context, BroadcastReceiver}
import android.app.ProgressDialog

class DeviceSelectionActivity extends SActivity {

  private case class Device(device: BluetoothDevice){
    override def toString = device.getName
  }

  private var receiverOption: Option[BroadcastReceiver] = None

  private var waitForBond: Option[(BluetoothDevice, ProgressDialog)] = None

	onCreate {

    theme = android.R.style.Theme_Holo
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
    Option(BluetoothAdapter.getDefaultAdapter) match {
      case Some(adapter) =>
        contentView = new SVerticalLayout {
          def startScan(){
            setProgressBarIndeterminate(true)
            setTitle("Scanning")
            adapter.startDiscovery()
          }
          STextView("Paired:")
          val pairedDevicesList = SListView()
          val pairedDevicesListAdapter = SArrayAdapter(adapter.getBondedDevices.toIndexedSeq.map(Device(_)) : _*)
          STextView("Found:")
          val foundDevicesListAdapter = new ArrayAdapter[Device](ctx, android.R.layout.simple_spinner_item)
          val foundDevicesList = SListView()
          for((list, adapter) <- Seq((pairedDevicesList, pairedDevicesListAdapter), (foundDevicesList, foundDevicesListAdapter))){
            list.adapter = adapter
            list.onItemClick{ (_: AdapterView[_], _: View, position: Int, _: Long) =>
              //val selectedDevice = listView.selectedItem.asInstanceOf[Device].device
              val selectedDevice = adapter.getItem(position).device
              if(selectedDevice.getBondState == BluetoothDevice.BOND_BONDED) {
                openUri(RemoteControlActivity(selectedDevice))
              }else{
                val d = spinnerDialog("Pairing...", "Waiting to be paired")
                waitForBond = Some(selectedDevice, d)
                val succeeded = selectedDevice.createBond()
                if(!succeeded){
                  d.hide()
                  alert(
                    "Fail",
                    "Can't pair with the device for whatever reason.\n" +
                      "Sorry for not giving you the reason, but Android didn't tell it." +
                      "So I don't suck. It is the Android who sucks!"
                  )
                }

              }
            }

          }
          val actionHandlers = Map[String, (Intent => Unit)](
            BluetoothDevice.ACTION_FOUND -> { i =>
              val device = i.getParcelableExtra[BluetoothDevice](BluetoothDevice.EXTRA_DEVICE)
              foundDevicesListAdapter.add(Device(device))
              toast(s"found: ${device.getName}")
            },
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> { i =>
              setProgressBarIndeterminateVisibility(false)
              setTitle("Scanning done")
            },
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> { i =>
              for{
                (device, dialog) <- waitForBond
                if device.getBondState == BluetoothDevice.BOND_BONDED
              }{
                dialog.hide()
                openUri(RemoteControlActivity(device))
              }
            }
          )
          val receiver = new BroadcastReceiver {
            override def onReceive(c: Context, i: Intent){
              actionHandlers.get(i.getAction) match {
                case Some(actionHandler) => actionHandler(i)
                case None => toast("Unexpected intent received: "+i)
                  warn("Unexpected intent received: "+i)
              }
            }
          }
          for(key <- actionHandlers.keys){
            registerReceiver(receiver, new IntentFilter(key))
          }
          receiverOption = Some(receiver)
          startScan()
          //-SSeekBar().
          //val sb = SSeekBar()
          //sb.onProgressChanged((b: SeekBar, num: Int, fromUser: Boolean) => toast(s"$b, $num, $fromUser"))
    			//SButton(R.string.red, toast("red"))
        }
      case None =>
        alert("Error", "Yay, you do not seem to have Bluetooth-enabled device", {finish()})

    }


	}

  onDestroy{
    for(reciever <- receiverOption){
      unregisterReceiver(reciever)
    }
  }

}
