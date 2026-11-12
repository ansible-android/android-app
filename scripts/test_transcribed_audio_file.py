"""Run the production TL reader/writer and vector code on a host JDK.

Only Android logging/native platform dependencies are replaced. The update
classes, dispatch arms, byte reader/writer and vector parser come from sources.
Optional stdin frames stay in memory and are never printed or saved.
"""
import argparse
import base64
import json
import re
import shutil
import struct
import subprocess
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "Ansible/src/main/java/org/ansible/asnet"


def java_class(source, name):
    start = source.index("    public static class " + name + " ")
    end = source.index("{", start) + 1
    depth = 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    return source[start:end]


def tl_string(value):
    raw = value.encode("utf-8")
    header = bytes([len(raw)]) if len(raw) < 254 else b"\xfe" + len(raw).to_bytes(3, "little")
    data = header + raw
    return data + bytes(-len(data) % 4)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--frames-stdin", action="store_true")
    args = parser.parse_args()
    frames = json.loads(__import__("sys").stdin.read()) if args.frames_stdin else []
    source = (SRC / "tl/TL_update.java").read_text(encoding="utf-8")
    factory = (SRC / "TLRPC.java").read_text(encoding="utf-8")
    names = ["TL_updateTranscribedAudioFile", "TL_updateUser"]
    arms = []
    for name in names:
        arm = re.search(r"case TL_update\." + name + r"\.constructor:\s*return new TL_update\." + name + r"\(\);", factory)
        assert arm, "Missing production Update dispatch: " + name
        arms.append(arm.group())
    with tempfile.TemporaryDirectory(prefix="ansible-asr-wire-") as temporary:
        root = Path(temporary)

        def write(path, text):
            target = root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text, encoding="utf-8")

        for name in ["InputSerializedData", "OutputSerializedData", "AbstractSerializedData", "SerializedData", "TLDataSourceType", "Vector"]:
            write("org/ansible/asnet/" + name + ".java", (SRC / (name + ".java")).read_text(encoding="utf-8"))
        write("org/ansible/asnet/tl/TL_update.java", "package org.ansible.asnet.tl; import org.ansible.asnet.*; public class TL_update {\n" + "\n".join(java_class(source, name) for name in names) + "\n}")
        write("org/ansible/asnet/TLObject.java", """package org.ansible.asnet;
public class TLObject {
 public static final int FLAG_0=1;
 public static boolean hasFlag(int value,int bit){return (value&bit)!=0;}
 public static int setFlag(int value,int bit,boolean on){return on?value|bit:value&~bit;}
 public void readParams(InputSerializedData s,boolean e){}
 public void serializeToStream(OutputSerializedData s){}
}""")
        write("org/ansible/asnet/NativeByteBuffer.java", "package org.ansible.asnet; public class NativeByteBuffer {}")
        write("org/ansible/asnet/TLParseException.java", "package org.ansible.asnet; public class TLParseException extends RuntimeException {public static void doThrowOrLog(InputSerializedData s,String n,int c,boolean e){if(e)throw new TLParseException();}}")
        write("org/ansible/messenger/BuildVars.java", "package org.ansible.messenger; public class BuildVars {public static final boolean LOGS_ENABLED=false;}")
        write("org/ansible/messenger/FileLog.java", "package org.ansible.messenger; public class FileLog {public static void e(Object o){}}")
        write("org/ansible/messenger/Utilities.java", "package org.ansible.messenger; public class Utilities {public interface Callback<T>{void run(T v);} public interface CallbackReturn<A,T>{T run(A a);}}")
        write("org/ansible/asnet/TLRPC.java", """package org.ansible.asnet;
import org.ansible.asnet.tl.TL_update;
public class TLRPC {public static class Update extends TLObject {
 static boolean baseline=false;
 public static Update TLdeserialize(InputSerializedData s,int c,boolean e){
  if(baseline && c==0x734c2e7b)throw new TLParseException();
  Update result=create(c);if(result==null)throw new TLParseException();result.readParams(s,e);return result;
 }
 private static Update create(int c){switch(c){""" + "\n".join(arms) + "default:return null;}}}}")
        write("org/ansible/asnet/WireProbe.java", """package org.ansible.asnet;
import java.util.*;import java.nio.charset.StandardCharsets;import org.ansible.asnet.tl.TL_update;
public class WireProbe {
 static void check(boolean ok){if(!ok)throw new AssertionError("TL wire mismatch");}
 public static void main(String[] args)throws Exception{
  if(args[0].equals("frame")){
   byte[] raw=System.in.readAllBytes();SerializedData in=new SerializedData(raw);
   TLRPC.Update item=TLRPC.Update.TLdeserialize(in,in.readInt32(true),true);
   check(item instanceof TL_update.TL_updateTranscribedAudioFile);check(in.remaining()==0);
   SerializedData out=new SerializedData();item.serializeToStream(out);check(Arrays.equals(raw,out.toByteArray()));
   System.out.println("PASS production file update frame");return;
  }
  byte[] raw=HexFormat.of().parseHex(args[0]);String text=new String(Base64.getDecoder().decode(args[1]),StandardCharsets.UTF_8);
  TLRPC.Update.baseline=true;boolean failed=false;
  try{Vector.deserialize(new SerializedData(raw),TLRPC.Update::TLdeserialize,true);}catch(TLParseException expected){failed=true;}
  check(failed);TLRPC.Update.baseline=false;
  SerializedData in=new SerializedData(raw);ArrayList<TLRPC.Update> items=Vector.deserialize(in,TLRPC.Update::TLdeserialize,true);
  check(items.size()==3);check(in.remaining()==0);
  TL_update.TL_updateTranscribedAudioFile a=(TL_update.TL_updateTranscribedAudioFile)items.get(0);
  TL_update.TL_updateTranscribedAudioFile b=(TL_update.TL_updateTranscribedAudioFile)items.get(1);
  check(a.pending && !b.pending);check(a.flags==129 && b.flags==128);
  check(a.transcription_id==Long.MAX_VALUE && b.transcription_id==Long.MIN_VALUE+1);
  check(a.text.equals(text) && b.text.equals(text));check(((TL_update.TL_updateUser)items.get(2)).user_id==42);
  SerializedData out=new SerializedData();Vector.serialize(out,items);check(Arrays.equals(raw,out.toByteArray()));
  System.out.println("PASS pending/final/vector continuation/round-trip; baseline rejected");
 }
}""")
        sources = [str(p) for p in root.rglob("*.java")]
        subprocess.run([shutil.which("javac") or "javac", "-encoding", "UTF-8", "-d", str(root), *sources], check=True)
        command = [shutil.which("java") or "java", "-cp", str(root), "org.ansible.asnet.WireProbe"]
        for text in ["", "hello", "Проверка 🎙️", "a" * 253, "a" * 254, "я" * 200]:
            raw = struct.pack("<II", 0x1CB5C415, 3)
            for flags, identity in [(129, 2**63 - 1), (128, -(2**63) + 1)]:
                raw += struct.pack("<IIq", 0x734C2E7B, flags, identity) + tl_string(text)
            raw += struct.pack("<Iq", 0x20529438, 42)
            subprocess.run([*command, raw.hex(), base64.b64encode(text.encode()).decode()], check=True)
        for frame in frames:
            subprocess.run([*command, "frame"], input=base64.b64decode(frame, validate=True), check=True)
        print("PASS 6 synthetic vectors and", len(frames), "captured frames")


if __name__ == "__main__":
    main()
