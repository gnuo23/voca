"use client";

type ToeicAudioPlayerProps = {
  src: string;
  label?: string;
};

export function ToeicAudioPlayer({ src, label }: ToeicAudioPlayerProps) {
  return (
    <div className="toeic-audio">
      {label ? <span className="toeic-audio-label">{label}</span> : null}
      <audio controls preload="none" src={src} className="toeic-audio-el">
        Trình duyệt không hỗ trợ phát audio.
      </audio>
    </div>
  );
}
