require 'nxt'
require 'pry'

class MessageWrite < DirectCommand
  include MessageTranslator

  attr_reader :message

  def initialize(mailbox, message)
    super()
    @command = 0x09
    @mailbox = mailbox
    @message = message
  end

  def as_bytes
    bytes = super
    payload = message_as_bytes
    len = payload.count
    mbox = [@mailbox].pack('C').bytes.first
    bytes + [mbox, len] + payload
  end

  private

  def message_as_bytes
    r = case message
    when Numeric
      string_as_bytes(message.to_s)
    when String
      string_as_bytes(message)
    when Array
      message
    else
      raise "Unsupported message payload: #{message.inspect}"
    end
    puts r.inspect
    r
  end
end

class NXT
  def message_write(mailbox, msg)
    send_message(MessageWrite.new(mailbox, msg), DirectCommandReply)
  end
end

class Legway
  class DSL
    FORWARD_MOTION_MAILBOX = 1

    def initialize(nxt)
      @nxt = nxt
    end

    def forward(velocity)
      @nxt.message_write(FORWARD_MOTION_MAILBOX, velocity)
    end
  end

  def initialize
    @nxt = NXT.new('/dev/tty.NXT-DevB')
  end

  def repl
    connect
    DSL.new(@nxt).instance_eval do
      binding.pry
    end
    disconnect
  end

  private

  def connect
    @nxt.connect
    @nxt.play_tone(440, 100)
    @nxt.play_tone(660, 100)
  end

  def disconnect
    @nxt.play_tone(660, 100)
    @nxt.play_tone(440, 100)
    @nxt.disconnect
  end
end

Legway.new.repl
