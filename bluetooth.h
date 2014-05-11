
/**
 *  Common setup
 *
 *  MASTER - BT channel of master device
 *  FORWARD_MOTION_MAILBOX - NXT mailbox used for forward motion messages
 *  DEADLINE_UPPER_BOUND - Threshold to secure case of too high clock drift
 */
#define MASTER 0
#define FORWARD_MOTION_MAILBOX 1
#define DEADLINE_UPPER_BOUND 1000

/** BT init'ed flag, timestamp of BT init (clock sync) */
bool bt_handshake = false;
unsigned long start_tick;

/** Last value of forward velocity command and it's time validity */
int received_forward_velicity = 0;
long valid_until = 0;

/** Output of forward velocity command with validity taken into the account */
int forward_velocity = 0;

/** Message counter and raw command payload, for debugging purposes */
int bt_messages = 0;
string bt_last_payload;

/**
 * BT Communication task
 *
 * Takes care of picking up message from mailbox and parsing it's payload
 *
 * Scheduling: In order to avoid maibox overflow, this task has to be scheduled
 * with period shorter than than the period of bluetooth loop running on
 * the MASTER device.
 *
 * Outputs: received_forward_velicity, valid_until
 */
task btComm() {
  string payload;
  int delimiter_pos;

  if(ReceiveRemoteString(FORWARD_MOTION_MAILBOX, true, payload) == NO_ERR) {
    if (!bt_handshake) {
      start_tick = CurrentTick();
      bt_handshake = true;
    }
    bt_messages++;

    if(delimiter_pos = Pos(" ", payload) != -1) {
      received_forward_velicity = atoi(payload);
      valid_until = start_tick + atol(RightStr(payload, StrLen(payload) - delimiter_pos - 2));
    }
    bt_last_payload = payload;
  }
}

/**
 * Command validity task
 *
 * Ensures no command effects the device longer than it was allowed by it's
 * valid_until attribute. When there is no valid command it sets forward
 * velocity to 0.
 *
 * Scheduling: Period has to be shorter than period of validity of all
 * commands. Worst case: Task is executed right before command expires,
 * so the command stays effective until the next execution of this task.
 *
 * Output: forward_velocity
 */
task btController() {
  if(bt_handshake) {
    long relative_deadline = valid_until - CurrentTick();

    if(relative_deadline > 0) {
      if(relative_deadline < DEADLINE_UPPER_BOUND)
        forward_velocity = received_forward_velicity;
      else
        forward_velocity = 0;
    } else {
      forward_velocity = 0;
    }
  }
}

/**
 * BT Debugging task
 *
 * Prints varions information about BT module on the display.
 *
 * Scheduling: Not critical, recomended period should not be shorter than 1 s.
 *
 * Output: Display lines 2, 3, 5 and 6.
 */
task btDebug() {
  ClearLine(LCD_LINE2);
  TextOut(0, LCD_LINE2, "#" + NumToStr(bt_messages) + ": " + bt_last_payload);

  ClearLine(LCD_LINE3);

  ClearLine(LCD_LINE4);
  TextOut(0, LCD_LINE4, "V: " + NumToStr(forward_velocity));
  ClearLine(LCD_LINE5);
  TextOut(0, LCD_LINE5, "S: " + NumToStr(start_tick));
  ClearLine(LCD_LINE6);
  TextOut(0, LCD_LINE6, "T: " + NumToStr(valid_until));
}
