require 'spibble/database'

require 'taglib'

module Spibble
  module Import
    ImportError = Class.new(StandardError)

    module_function

    def directory(dir)
      files = Dir.entries(dir).map {|f| File.join(dir, f) }.select {|f| File.file? f }
      files(files)
    end

    def files(files)
      tracks = []
      files.each do |file|
        # This is basically just so we can work with the data outside of
        # TagLib's nasty API
        TagLib::FileRef.open(file) do |f|
          tracks << {title: f.tag.title, artist: f.tag.artist, album: f.tag.album,
            number: f.tag.track, length: f.audio_properties.length} unless f.null?
        end
      end

      album = tracks.map {|t| t[:album] }.uniq
      raise ImportError, 'not all tracks are from same album' unless album.length == 1
      album = album.first

      artist = tracks.map {|t| t[:artist] }.uniq
      raise ImportError, 'not all tracks are by same artist' unless artist.length == 1
      artist = artist.first

      tracks.sort_by! {|t| t[:number] }

      Album.new(album, artist, tracks.map {|t| Track.new(t[:number], t[:title], t[:length]) }, [])
    end
  end
end
