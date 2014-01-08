package com.egergo.ncskeleton;

public class Rtp {

    
    public static class RtpPacket {
        private boolean padding;
        private boolean marker;
        private byte payloadType;
        private int sequenceNumber;
        private long timestamp;
        private long ssrc;
        
        public boolean isPadding() {
            return padding;
        }

        public void setPadding(boolean padding) {
            this.padding = padding;
        }

        public boolean isMarker() {
            return marker;
        }

        public void setMarker(boolean marker) {
            this.marker = marker;
        }

        public byte getPayloadType() {
            return payloadType;
        }

        public void setPayloadType(byte payloadType) {
            if (payloadType < 0) {
                throw new IllegalArgumentException("Payload Type must be between 0 and 127");
            }
            this.payloadType = payloadType;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        public void setSequenceNumber(int sequenceNumber) {
            if (sequenceNumber < 0 || sequenceNumber > 65535) {
                throw new IllegalArgumentException("Sequence Number must be between 0 and 65535");
            }
            this.sequenceNumber = sequenceNumber;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            if (timestamp < 0 || timestamp > 0xFFFFFFFFl) {
                throw new IllegalArgumentException("Timestamp must be between 0 and 0xFFFFFFFFl");
            }            
            this.timestamp = timestamp;
        }

        public long getSsrc() {
            return ssrc;
        }

        public void setSsrc(long ssrc) {
            if (ssrc < 0 || ssrc > 0xFFFFFFFFl) {
                throw new IllegalArgumentException("SSRC must be between 0 and 0xFFFFFFFFl");
            }
            this.ssrc = ssrc;
        }

        public int serialize(byte[] result, byte[] payload, int offset, int length) {
            if (payload.length < offset + length) {
                throw new IllegalArgumentException("Invalid position in payload");
            }
            
            int resultLength = 12 + length;
            
            if (result.length < resultLength) {
                throw new IllegalArgumentException("Target buffer too small");
            }
            
            result[0] = (byte) (2 | (padding ? 0 : 0x04));            
            result[1] = (byte) ((marker ? 1 : 0) | ((payloadType & 0x7F) << 1));
            
            result[2] = (byte) ((sequenceNumber & 0xFF00) >> 8);
            result[3] = (byte) (sequenceNumber & 0xFF);
            
            result[4] = (byte) ((timestamp & 0xFF000000) >> 24);
            result[5] = (byte) ((timestamp & 0xFF0000) >> 16);
            result[6] = (byte) ((timestamp & 0xFF00) >> 8);
            result[7] = (byte) (timestamp & 0xFF);
            
            result[8] = (byte) ((ssrc & 0xFF000000) >> 24);
            result[9] = (byte) ((ssrc & 0xFF0000) >> 16);
            result[10] = (byte) ((ssrc & 0xFF00) >> 8);
            result[11] = (byte) (ssrc & 0xFF);
            
            System.arraycopy(payload, offset, result, 12, length);
            
            return resultLength;
        }
        
        public int deserialize(byte[] packet, int offset, int length) {
            if (packet.length < offset + length) {
                throw new IllegalArgumentException("Invalid position in payload");
            }
            
            if (length < 12) {
                throw new IllegalArgumentException("Packet too short: " + packet.length);
            }
            
            if ((packet[offset + 0] & 0x03) != 2) {
                throw new IllegalArgumentException("Unknown RTP version: " + (packet[offset + 0] & 0x03));
            }
            
            padding = (packet[offset + 0] & 0x04) != 0;
            marker = (packet[offset + 1] & 0x01) != 0;
            payloadType = (byte) ((packet[offset + 1] & 0xFE) >> 1);
            sequenceNumber = ((packet[offset + 2] & 0xFF) << 8) | (packet[offset + 3] & 0xFF);
            timestamp = ((packet[offset + 4] & 0xFFl) << 24) | ((packet[offset + 5] & 0xFFl) << 16) | ((packet[offset + 6] & 0xFFl) << 8) | (packet[offset + 7] & 0xFFl);
            ssrc = ((packet[offset + 8] & 0xFFl) << 24) | ((packet[offset + 9] & 0xFFl) << 16) | ((packet[offset + 10] & 0xFFl) << 8) | (packet[offset + 11] & 0xFFl);
            
            return 12;
        }
        
        
    }
    
}
